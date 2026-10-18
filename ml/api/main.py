from pathlib import Path

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel


# ============================================================
# PATHS
# ============================================================

BASE_DIR = Path(__file__).resolve().parent.parent

MODEL_PATH = (
    BASE_DIR
    / "models"
    / "vendor_delivery_risk_model.pkl"
)


# ============================================================
# LOAD MODEL
# ============================================================

if not MODEL_PATH.exists():
    raise FileNotFoundError(
        f"Model not found at:\n{MODEL_PATH}"
    )

model = joblib.load(MODEL_PATH)


# ============================================================
# FASTAPI APP
# ============================================================

app = FastAPI(
    title="Vendor Delivery Risk Prediction API",
    description="ML API for predicting vendor delivery risk.",
    version="1.0.0"
)


# ============================================================
# REQUEST MODEL
# ============================================================

class PredictionRequest(BaseModel):

    category: str
    priority: str

    quantity: float
    unit_price: float
    total_amount: float
    planned_delivery_days: float

    previous_po_count: float
    previous_on_time_rate: float
    previous_average_delay: float
    previous_fulfillment_rate: float
    previous_quality_issue_rate: float

    recent_on_time_rate: float
    recent_average_delay: float
    recent_fulfillment_rate: float


# ============================================================
# HEALTH CHECK
# ============================================================

@app.get("/")
def home():
    return {
        "message": "Vendor Delivery Risk Prediction API is running",
        "model": "Random Forest - Balanced"
    }


# ============================================================
# MODEL INFORMATION
# ============================================================

@app.get("/model-info")
def model_info():

    return {
        "model": "Random Forest",
        "task": "Multiclass Delivery Risk Classification",
        "classes": [
            "Low",
            "Medium",
            "High"
        ],
        "features": 14,
        "model_file": "vendor_delivery_risk_model.pkl"
    }


# ============================================================
# PREDICTION
# ============================================================
@app.post("/predict")
def predict(request: PredictionRequest):

    try:

        input_data = pd.DataFrame([{
            "category": request.category,
            "priority": request.priority,
            "quantity": request.quantity,
            "unit_price": request.unit_price,
            "total_amount": request.total_amount,
            "planned_delivery_days": request.planned_delivery_days,

            "previous_po_count": request.previous_po_count,
            "previous_on_time_rate": request.previous_on_time_rate,
            "previous_average_delay": request.previous_average_delay,
            "previous_fulfillment_rate": request.previous_fulfillment_rate,
            "previous_quality_issue_rate": request.previous_quality_issue_rate,

            "recent_on_time_rate": request.recent_on_time_rate,
            "recent_average_delay": request.recent_average_delay,
            "recent_fulfillment_rate": request.recent_fulfillment_rate
        }])

        prediction = model.predict(input_data)[0]

        probability = model.predict_proba(input_data)[0]

        classes = model.classes_

        probabilities = {
            str(class_name): round(
                float(prob),
                4
            )
            for class_name, prob in zip(
                classes,
                probability
            )
        }

        risk_probability = probabilities.get(
            str(prediction),
            0.0
        )

        return {
            "delivery_risk": str(prediction),
            "risk_probability": risk_probability,
            "probabilities": probabilities
        }

    except Exception as e:

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )

