# train_model.py
#
# Final controlled model-improvement experiment
# Project: Intelligent Vendor Performance and Delivery Risk Management System

from pathlib import Path

import joblib
import pandas as pd

from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score
)
from sklearn.model_selection import (
    train_test_split,
    StratifiedKFold,
    cross_validate
)
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.tree import DecisionTreeClassifier


# ============================================================
# 1. PATHS
# ============================================================

BASE_DIR = Path(__file__).resolve().parent.parent

DATA_PATH = (
    BASE_DIR
    / "data"
    / "vendor_delivery_risk_dataset.csv"
)

MODEL_DIR = (
    BASE_DIR
    / "models"
)

MODEL_PATH = (
    MODEL_DIR
    / "vendor_delivery_risk_model.pkl"
)

COMPARISON_PATH = (
    MODEL_DIR
    / "model_comparison.csv"
)


# ============================================================
# 2. LOAD DATA
# ============================================================

if not DATA_PATH.exists():
    raise FileNotFoundError(
        f"Dataset not found:\n{DATA_PATH}\n"
        f"Run load_data.py first."
    )

df = pd.read_csv(DATA_PATH)

print("=" * 70)
print("VENDOR DELIVERY RISK MODEL TRAINING")
print("=" * 70)

print(f"\nDataset loaded successfully: {len(df)} records, {len(df.columns)} columns")


# ============================================================
# 3. TARGET
# ============================================================

TARGET = "delivery_risk"

if TARGET not in df.columns:
    raise ValueError(
        f"Target column '{TARGET}' was not found in the dataset."
    )

print("\nTarget distribution:")
print(df[TARGET].value_counts())


# ============================================================
# 4. FEATURES
# ============================================================

FEATURES = [
    "category",
    "priority",
    "quantity",
    "unit_price",
    "total_amount",
    "planned_delivery_days",

    # Historical vendor features
    "previous_po_count",
    "previous_on_time_rate",
    "previous_average_delay",
    "previous_fulfillment_rate",
    "previous_quality_issue_rate",

    # Recent vendor history
    "recent_on_time_rate",
    "recent_average_delay",
    "recent_fulfillment_rate",
]


missing_features = [
    feature
    for feature in FEATURES
    if feature not in df.columns
]

if missing_features:
    raise ValueError(
        f"Missing required feature columns: {missing_features}"
    )


X = df[FEATURES].copy()
y = df[TARGET].copy()


# ============================================================
# 5. FEATURE TYPES
# ============================================================

CATEGORICAL_FEATURES = [
    "category",
    "priority",
]

NUMERICAL_FEATURES = [
    "quantity",
    "unit_price",
    "total_amount",
    "planned_delivery_days",
    "previous_po_count",
    "previous_on_time_rate",
    "previous_average_delay",
    "previous_fulfillment_rate",
    "previous_quality_issue_rate",
    "recent_on_time_rate",
    "recent_average_delay",
    "recent_fulfillment_rate",
]


# ============================================================
# 6. PREPROCESSING
# ============================================================

numeric_pipeline = Pipeline(
    steps=[
        (
            "imputer",
            SimpleImputer(strategy="median")
        ),
        (
            "scaler",
            StandardScaler()
        ),
    ]
)


categorical_pipeline = Pipeline(
    steps=[
        (
            "imputer",
            SimpleImputer(strategy="most_frequent")
        ),
        (
            "encoder",
            OneHotEncoder(
                handle_unknown="ignore"
            )
        ),
    ]
)


preprocessor = ColumnTransformer(
    transformers=[
        (
            "numeric",
            numeric_pipeline,
            NUMERICAL_FEATURES
        ),
        (
            "categorical",
            categorical_pipeline,
            CATEGORICAL_FEATURES
        ),
    ]
)


# ============================================================
# 7. TRAIN / TEST SPLIT
# ============================================================

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    stratify=y,
    random_state=42
)

print("\nTraining records :", len(X_train))
print("Testing records  :", len(X_test))


# ============================================================
# 8. CROSS-VALIDATION
# ============================================================

cv = StratifiedKFold(
    n_splits=5,
    shuffle=True,
    random_state=42
)

scoring = {
    "accuracy": "accuracy",
    "precision": "precision_macro",
    "recall": "recall_macro",
    "f1": "f1_macro",
}


# ============================================================
# 9. MODEL CONFIGURATIONS
# ============================================================

models = {

    # --------------------------------------------------------
    # Logistic Regression
    # --------------------------------------------------------
    "Logistic Regression": LogisticRegression(
        max_iter=2000,
        class_weight="balanced",
        random_state=42
    ),

    # --------------------------------------------------------
    # Decision Tree
    # --------------------------------------------------------
    "Decision Tree": DecisionTreeClassifier(
        random_state=42,
        max_depth=6,
        min_samples_leaf=5,
        class_weight="balanced"
    ),

    # --------------------------------------------------------
    # Random Forest - Balanced
    # --------------------------------------------------------
    "Random Forest - Balanced": RandomForestClassifier(
        n_estimators=300,
        random_state=42,
        class_weight="balanced",
        min_samples_leaf=3,
        n_jobs=-1
    ),

    # --------------------------------------------------------
    # Random Forest - Medium Weighted
    # --------------------------------------------------------
    "Random Forest - Medium x2": RandomForestClassifier(
        n_estimators=300,
        random_state=42,
        class_weight={
            "Low": 1.0,
            "Medium": 2.0,
            "High": 1.0
        },
        min_samples_leaf=3,
        n_jobs=-1
    ),

    # --------------------------------------------------------
    # Random Forest - Medium Weighted Stronger
    # --------------------------------------------------------
    "Random Forest - Medium x3": RandomForestClassifier(
        n_estimators=300,
        random_state=42,
        class_weight={
            "Low": 1.0,
            "Medium": 3.0,
            "High": 1.0
        },
        min_samples_leaf=3,
        n_jobs=-1
    ),

    # --------------------------------------------------------
    # Gradient Boosting
    # --------------------------------------------------------
    "Gradient Boosting": GradientBoostingClassifier(
        random_state=42,
        n_estimators=150,
        learning_rate=0.05,
        max_depth=3
    ),
}


# ============================================================
# 10. CROSS-VALIDATION RESULTS
# ============================================================

results = []

print("\n" + "=" * 70)
print("5-FOLD CROSS-VALIDATION RESULTS")
print("=" * 70)


for model_name, model in models.items():

    pipeline = Pipeline(
        steps=[
            (
                "preprocessor",
                preprocessor
            ),
            (
                "model",
                model
            ),
        ]
    )

    cv_results = cross_validate(
        pipeline,
        X_train,
        y_train,
        cv=cv,
        scoring=scoring,
        n_jobs=1
    )

    accuracy_mean = cv_results["test_accuracy"].mean()
    accuracy_std = cv_results["test_accuracy"].std()

    precision_mean = cv_results["test_precision"].mean()
    precision_std = cv_results["test_precision"].std()

    recall_mean = cv_results["test_recall"].mean()
    recall_std = cv_results["test_recall"].std()

    f1_mean = cv_results["test_f1"].mean()
    f1_std = cv_results["test_f1"].std()

    results.append(
        {
            "Model": model_name,
            "CV Accuracy Mean": accuracy_mean,
            "CV Accuracy Std": accuracy_std,
            "CV Macro Precision Mean": precision_mean,
            "CV Macro Precision Std": precision_std,
            "CV Macro Recall Mean": recall_mean,
            "CV Macro Recall Std": recall_std,
            "CV Macro F1 Mean": f1_mean,
            "CV Macro F1 Std": f1_std,
        }
    )

    print(f"\n{model_name}")
    print(f"Accuracy       : {accuracy_mean:.4f} Â± {accuracy_std:.4f}")
    print(f"Macro Precision: {precision_mean:.4f} Â± {precision_std:.4f}")
    print(f"Macro Recall   : {recall_mean:.4f} Â± {recall_std:.4f}")
    print(f"Macro F1       : {f1_mean:.4f} Â± {f1_std:.4f}")


# ============================================================
# 11. SELECT BEST MODEL
# ============================================================

results_df = pd.DataFrame(results)

results_df = results_df.sort_values(
    by="CV Macro F1 Mean",
    ascending=False
).reset_index(drop=True)


best_model_name = results_df.iloc[0]["Model"]

best_model = models[best_model_name]


print("\n" + "=" * 70)
print("MODEL SELECTION")
print("=" * 70)

print(
    f"\nSelected model based on CV Macro F1: "
    f"{best_model_name}"
)


# ============================================================
# 12. TRAIN BEST MODEL
# ============================================================

final_pipeline = Pipeline(
    steps=[
        (
            "preprocessor",
            preprocessor
        ),
        (
            "model",
            best_model
        ),
    ]
)

final_pipeline.fit(
    X_train,
    y_train
)


# ============================================================
# 13. FINAL TEST EVALUATION
# ============================================================

y_pred = final_pipeline.predict(X_test)


test_accuracy = accuracy_score(
    y_test,
    y_pred
)

test_precision = precision_score(
    y_test,
    y_pred,
    average="macro",
    zero_division=0
)

test_recall = recall_score(
    y_test,
    y_pred,
    average="macro",
    zero_division=0
)

test_f1 = f1_score(
    y_test,
    y_pred,
    average="macro",
    zero_division=0
)


print("\n" + "=" * 70)
print("FINAL TEST RESULTS")
print("=" * 70)

print(f"\nAccuracy       : {test_accuracy:.4f}")
print(f"Macro Precision: {test_precision:.4f}")
print(f"Macro Recall   : {test_recall:.4f}")
print(f"Macro F1       : {test_f1:.4f}")


# ============================================================
# 14. CLASSIFICATION REPORT
# ============================================================

print("\nClassification Report:")

print(
    classification_report(
        y_test,
        y_pred,
        labels=["Low", "Medium", "High"],
        zero_division=0
    )
)


# ============================================================
# 15. CONFUSION MATRIX
# ============================================================

cm = confusion_matrix(
    y_test,
    y_pred,
    labels=["Low", "Medium", "High"]
)

print("Confusion Matrix")
print(
    "                Predicted"
)
print(
    "              Low  Medium  High"
)

print(
    f"Actual Low    {cm[0][0]:3d}   {cm[0][1]:3d}   {cm[0][2]:3d}"
)

print(
    f"Actual Medium {cm[1][0]:3d}   {cm[1][1]:3d}   {cm[1][2]:3d}"
)

print(
    f"Actual High   {cm[2][0]:3d}   {cm[2][1]:3d}   {cm[2][2]:3d}"
)


# ============================================================
# 16. ADD FINAL TEST RESULTS TO COMPARISON TABLE
# ============================================================

results_df["Final Test Accuracy"] = None
results_df["Final Test Macro Precision"] = None
results_df["Final Test Macro Recall"] = None
results_df["Final Test Macro F1"] = None

best_index = results_df.index[
    results_df["Model"] == best_model_name
][0]

results_df.loc[
    best_index,
    "Final Test Accuracy"
] = test_accuracy

results_df.loc[
    best_index,
    "Final Test Macro Precision"
] = test_precision

results_df.loc[
    best_index,
    "Final Test Macro Recall"
] = test_recall

results_df.loc[
    best_index,
    "Final Test Macro F1"
] = test_f1


# ============================================================
# 17. SAVE MODEL
# ============================================================

MODEL_DIR.mkdir(
    parents=True,
    exist_ok=True
)

joblib.dump(
    final_pipeline,
    MODEL_PATH
)


# ============================================================
# 18. SAVE MODEL COMPARISON
# ============================================================

results_df.to_csv(
    COMPARISON_PATH,
    index=False
)


# ============================================================
# 19. FINAL SUMMARY
# ============================================================

print("\n" + "=" * 70)
print("MODEL SAVED")
print("=" * 70)

print(
    f"\nModel saved to:"
    f"\n{MODEL_PATH}"
)

print(
    f"\nModel comparison saved to:"
    f"\n{COMPARISON_PATH}"
)

print(
    f"\nBest model: {best_model_name}"
)

print(
    f"Test accuracy: {test_accuracy:.4f}"
)

print(
    f"Test macro F1: {test_f1:.4f}"
)

print("\nTraining completed successfully.")
