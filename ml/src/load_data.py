import os
from pathlib import Path

import pandas as pd
from dotenv import load_dotenv
from sqlalchemy import create_engine, text


# ============================================================
# CONFIGURATION
# ============================================================

# Load environment variables from D:\VendorManagementAI\.env
load_dotenv()


DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "3306")
DB_NAME = os.getenv("DB_NAME", "vendor_management_db")


if not DB_USER or not DB_PASSWORD:
    raise ValueError(
        "Database credentials not found. "
        "Please check the .env file."
    )


DB_URL = (
    f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}"
    f"@{DB_HOST}:{DB_PORT}/{DB_NAME}"
)


# ------------------------------------------------------------
# IMPORTANT:
# Build paths relative to the project root so the script works
# correctly even when executed from another directory.
# ------------------------------------------------------------

BASE_DIR = Path(__file__).resolve().parent.parent

OUTPUT_PATH = (
    BASE_DIR
    / "data"
    / "vendor_delivery_risk_dataset.csv"
)


EXPECTED_PURCHASE_ORDERS = 500
EXPECTED_QUALITY_EVALUATIONS = 500
EXPECTED_VENDORS = 10


# ============================================================
# DATABASE CONNECTION
# ============================================================

print("=" * 70)
print("VENDOR DELIVERY RISK - DATA PREPARATION")
print("=" * 70)

engine = create_engine(DB_URL)


# ============================================================
# LOAD DATA FROM MYSQL
# ============================================================

with engine.connect() as connection:

    purchase_orders = pd.read_sql(
        text(
            """
            SELECT
                id,
                po_number,
                vendor_id,
                order_date,
                expected_delivery_date,
                actual_delivery_date,
                priority,
                quantity,
                actual_delivered_quantity,
                unit_price,
                total_amount,
                status
            FROM purchase_orders
            """
        ),
        connection
    )

    vendors = pd.read_sql(
        text(
            """
            SELECT
                id,
                vendor_name,
                category,
                status
            FROM vendors
            """
        ),
        connection
    )

    quality_evaluations = pd.read_sql(
        text(
            """
            SELECT
                id,
                purchase_order_id,
                evaluation_date,
                issue_count,
                quality_status,
                severity
            FROM quality_evaluations
            """
        ),
        connection
    )


# ============================================================
# DATABASE RECORD VALIDATION
# ============================================================

print()
print("Database Records")
print("-" * 70)

print(f"Purchase Orders       : {len(purchase_orders)}")
print(f"Vendors               : {len(vendors)}")
print(f"Quality Evaluations   : {len(quality_evaluations)}")


if len(purchase_orders) != EXPECTED_PURCHASE_ORDERS:
    raise ValueError(
        f"Expected {EXPECTED_PURCHASE_ORDERS} purchase orders, "
        f"but found {len(purchase_orders)}."
    )


if len(quality_evaluations) != EXPECTED_QUALITY_EVALUATIONS:
    raise ValueError(
        f"Expected {EXPECTED_QUALITY_EVALUATIONS} quality evaluations, "
        f"but found {len(quality_evaluations)}."
    )


if len(vendors) != EXPECTED_VENDORS:
    raise ValueError(
        f"Expected {EXPECTED_VENDORS} vendors, "
        f"but found {len(vendors)}."
    )


print("Database record count check: PASSED")


# ============================================================
# RENAME PRIMARY KEYS
# ============================================================

purchase_orders = purchase_orders.rename(
    columns={
        "id": "po_id"
    }
)


vendors = vendors.rename(
    columns={
        "id": "vendor_table_id"
    }
)


# ============================================================
# MERGE VENDOR INFORMATION
# ============================================================

df = purchase_orders.merge(
    vendors[
        [
            "vendor_table_id",
            "vendor_name",
            "category"
        ]
    ],
    left_on="vendor_id",
    right_on="vendor_table_id",
    how="left"
)


# ============================================================
# MERGE QUALITY INFORMATION
# ============================================================

quality_evaluations = quality_evaluations.rename(
    columns={
        "purchase_order_id": "po_id"
    }
)


df = df.merge(
    quality_evaluations[
        [
            "po_id",
            "issue_count",
            "quality_status",
            "severity"
        ]
    ],
    on="po_id",
    how="left"
)


# ============================================================
# MERGE VALIDATION
# ============================================================

if len(df) != EXPECTED_PURCHASE_ORDERS:
    raise ValueError(
        "Merge changed record count. "
        f"Expected {EXPECTED_PURCHASE_ORDERS}, "
        f"found {len(df)}."
    )


print("Merge validation: PASSED")


# ============================================================
# REQUIRED COLUMN CHECK
# ============================================================

required_columns = [
    "po_id",
    "po_number",
    "vendor_id",
    "vendor_name",
    "category",
    "order_date",
    "expected_delivery_date",
    "actual_delivery_date",
    "priority",
    "quantity",
    "actual_delivered_quantity",
    "unit_price",
    "total_amount",
    "status",
    "issue_count",
    "quality_status",
    "severity"
]


missing_columns = [
    column
    for column in required_columns
    if column not in df.columns
]


if missing_columns:
    raise ValueError(
        f"Missing required columns: {missing_columns}"
    )


print("Required column check: PASSED")


# ============================================================
# DATE CONVERSION
# ============================================================

date_columns = [
    "order_date",
    "expected_delivery_date",
    "actual_delivery_date"
]


for column in date_columns:

    df[column] = pd.to_datetime(
        df[column],
        errors="coerce"
    )


# ============================================================
# BASIC DATA VALIDATION
# ============================================================

if df["order_date"].isna().any():
    raise ValueError(
        "Missing order_date detected."
    )


if df["expected_delivery_date"].isna().any():
    raise ValueError(
        "Missing expected_delivery_date detected."
    )


if df["actual_delivery_date"].isna().any():
    raise ValueError(
        "Missing actual_delivery_date detected."
    )


if (df["quantity"] <= 0).any():
    raise ValueError(
        "Invalid quantity detected."
    )


if (df["actual_delivered_quantity"] < 0).any():
    raise ValueError(
        "Negative actual delivered quantity detected."
    )


print("Basic data validation: PASSED")


# ============================================================
# DELIVERY OUTCOME CALCULATIONS
# ============================================================

df["delivery_delay"] = (
    df["actual_delivery_date"]
    - df["expected_delivery_date"]
).dt.days


df["fulfillment_rate"] = (
    df["actual_delivered_quantity"]
    / df["quantity"]
) * 100


# ============================================================
# DELIVERY RISK LABEL
# ============================================================
#
# HIGH:
#   3 or more days late
#   OR fulfillment below 90%
#
# MEDIUM:
#   1-2 days late
#
# LOW:
#   On time or early with acceptable fulfillment
#
# These labels are created from actual delivery outcomes.
#
# IMPORTANT:
# These outcome values are NOT used as model inputs.
# ============================================================

def assign_risk(row):

    if (
        row["delivery_delay"] >= 3
        or row["fulfillment_rate"] < 90
    ):
        return "High"

    elif row["delivery_delay"] >= 1:
        return "Medium"

    else:
        return "Low"


df["delivery_risk"] = df.apply(
    assign_risk,
    axis=1
)


# ============================================================
# SORT CHRONOLOGICALLY
# ============================================================

df = df.sort_values(
    by=[
        "vendor_id",
        "order_date",
        "po_id"
    ]
).reset_index(drop=True)


# ============================================================
# HISTORICAL VENDOR FEATURES
# ============================================================

print()
print("Creating historical vendor features...")


historical_features = []


for index, current_row in df.iterrows():

    vendor_id = current_row["vendor_id"]
    current_date = current_row["order_date"]


    # --------------------------------------------------------
    # IMPORTANT LEAKAGE PROTECTION
    # --------------------------------------------------------
    #
    # Only orders placed BEFORE the current order are used.
    #
    # The current order's outcome is never used to calculate
    # its historical features.
    #
    # Orders with the same order_date are also excluded.
    # --------------------------------------------------------

    previous_orders = df[
        (df["vendor_id"] == vendor_id)
        &
        (df["order_date"] < current_date)
    ]


    # ========================================================
    # NO PREVIOUS HISTORY
    # ========================================================

    if len(previous_orders) == 0:

        previous_po_count = 0

        previous_on_time_rate = 0.0

        previous_average_delay = 0.0

        previous_fulfillment_rate = 100.0

        previous_quality_issue_rate = 0.0


        # Recent-history neutral values

        recent_on_time_rate = 0.0

        recent_average_delay = 0.0

        recent_fulfillment_rate = 100.0


    # ========================================================
    # PREVIOUS HISTORY EXISTS
    # ========================================================

    else:

        # ----------------------------------------------------
        # OVERALL HISTORICAL FEATURES
        # ----------------------------------------------------

        previous_po_count = len(
            previous_orders
        )


        previous_on_time_rate = (
            (
                previous_orders["delivery_delay"] <= 0
            ).mean()
            * 100
        )


        previous_average_delay = (
            previous_orders["delivery_delay"].mean()
        )


        previous_fulfillment_rate = (
            previous_orders[
                "actual_delivered_quantity"
            ].sum()
            /
            previous_orders[
                "quantity"
            ].sum()
        ) * 100


        previous_quality_issue_rate = (
            (
                previous_orders["issue_count"] > 0
            ).mean()
            * 100
        )


        # ----------------------------------------------------
        # RECENT 5 PURCHASE ORDERS
        # ----------------------------------------------------
        #
        # We take only the five most recent completed orders
        # BEFORE the current PO.
        #
        # This gives the model a short-term picture of vendor
        # behavior in addition to the overall history.
        # ----------------------------------------------------

        recent_orders = (
            previous_orders
            .sort_values(
                by=[
                    "order_date",
                    "po_id"
                ],
                ascending=[
                    False,
                    False
                ]
            )
            .head(5)
        )


        recent_on_time_rate = (
            (
                recent_orders["delivery_delay"] <= 0
            ).mean()
            * 100
        )


        recent_average_delay = (
            recent_orders["delivery_delay"].mean()
        )


        recent_fulfillment_rate = (
            recent_orders[
                "actual_delivered_quantity"
            ].sum()
            /
            recent_orders[
                "quantity"
            ].sum()
        ) * 100


    # ========================================================
    # STORE FEATURES
    # ========================================================

    historical_features.append(
        {
            "previous_po_count":
                previous_po_count,

            "previous_on_time_rate":
                previous_on_time_rate,

            "previous_average_delay":
                previous_average_delay,

            "previous_fulfillment_rate":
                previous_fulfillment_rate,

            "previous_quality_issue_rate":
                previous_quality_issue_rate,

            "recent_on_time_rate":
                recent_on_time_rate,

            "recent_average_delay":
                recent_average_delay,

            "recent_fulfillment_rate":
                recent_fulfillment_rate
        }
    )


# ============================================================
# ADD HISTORICAL FEATURES TO DATAFRAME
# ============================================================

historical_df = pd.DataFrame(
    historical_features
)


df = pd.concat(
    [
        df.reset_index(drop=True),
        historical_df.reset_index(drop=True)
    ],
    axis=1
)


# ============================================================
# PLANNED DELIVERY DAYS
# ============================================================

df["planned_delivery_days"] = (
    df["expected_delivery_date"]
    - df["order_date"]
).dt.days


# ============================================================
# FINAL DATASET COLUMNS
# ============================================================

dataset_columns = [

    # --------------------------------------------------------
    # IDENTIFICATION
    # --------------------------------------------------------

    "po_id",
    "po_number",
    "vendor_id",
    "vendor_name",

    # --------------------------------------------------------
    # VENDOR / ORDER INFORMATION
    # --------------------------------------------------------

    "category",
    "order_date",
    "expected_delivery_date",
    "actual_delivery_date",
    "priority",
    "quantity",
    "actual_delivered_quantity",
    "unit_price",
    "total_amount",
    "status",

    # --------------------------------------------------------
    # QUALITY INFORMATION
    # --------------------------------------------------------

    "issue_count",
    "quality_status",
    "severity",

    # --------------------------------------------------------
    # ACTUAL OUTCOME INFORMATION
    # --------------------------------------------------------

    "delivery_delay",
    "fulfillment_rate",

    # --------------------------------------------------------
    # OVERALL HISTORICAL FEATURES
    # --------------------------------------------------------

    "previous_po_count",
    "previous_on_time_rate",
    "previous_average_delay",
    "previous_fulfillment_rate",
    "previous_quality_issue_rate",

    # --------------------------------------------------------
    # RECENT 5-PO FEATURES
    # --------------------------------------------------------

    "recent_on_time_rate",
    "recent_average_delay",
    "recent_fulfillment_rate",

    # --------------------------------------------------------
    # CURRENT ORDER PLANNING FEATURE
    # --------------------------------------------------------

    "planned_delivery_days",

    # --------------------------------------------------------
    # TARGET
    # --------------------------------------------------------

    "delivery_risk"
]


df = df[dataset_columns]


# ============================================================
# FINAL RECORD COUNT VALIDATION
# ============================================================

if len(df) != EXPECTED_PURCHASE_ORDERS:

    raise ValueError(
        "Final record count incorrect. "
        f"Expected {EXPECTED_PURCHASE_ORDERS}, "
        f"found {len(df)}."
    )


print(
    "Final record count check: PASSED"
)


# ============================================================
# DUPLICATE PO CHECK
# ============================================================

if df["po_id"].duplicated().any():

    raise ValueError(
        "Duplicate PO IDs detected."
    )


print(
    "Duplicate PO check: PASSED"
)


# ============================================================
# MISSING VALUE CHECK
# ============================================================

missing_values = df.isnull().sum()

missing_values = missing_values[
    missing_values > 0
]


if len(missing_values) > 0:

    print()
    print("Missing values detected:")
    print(missing_values)

    raise ValueError(
        "Dataset contains missing values."
    )


print(
    "Missing value check: PASSED"
)


# ============================================================
# RISK LABEL VALIDATION
# ============================================================

valid_risks = {
    "Low",
    "Medium",
    "High"
}


actual_risks = set(
    df["delivery_risk"].unique()
)


if not actual_risks.issubset(valid_risks):

    raise ValueError(
        f"Unexpected risk labels: {actual_risks}"
    )


print(
    "Risk label validation: PASSED"
)


# ============================================================
# DISPLAY FINAL DATASET INFORMATION
# ============================================================

print()
print("=" * 70)
print("FINAL DATASET")
print("=" * 70)


print(
    f"Total records  : {len(df)}"
)


print(
    f"Total columns  : {len(df.columns)}"
)


# ============================================================
# RISK DISTRIBUTION
# ============================================================

print()
print("Delivery Risk Distribution:")

print(
    df["delivery_risk"]
    .value_counts()
)


print()
print("Delivery Risk Percentage:")

print(
    (
        df["delivery_risk"]
        .value_counts(
            normalize=True
        )
        * 100
    ).round(2)
)


# ============================================================
# RECENT HISTORY SAMPLE
# ============================================================

print()
print("Recent History Feature Sample:")


print(
    df[
        [
            "po_number",
            "vendor_name",
            "previous_po_count",
            "previous_on_time_rate",
            "recent_on_time_rate",
            "recent_average_delay",
            "recent_fulfillment_rate",
            "delivery_risk"
        ]
    ]
    .head(10)
    .to_string(index=False)
)


# ============================================================
# SAVE DATASET
# ============================================================

OUTPUT_PATH.parent.mkdir(
    parents=True,
    exist_ok=True
)


df.to_csv(
    OUTPUT_PATH,
    index=False
)


print()
print(
    f"Dataset saved to: {OUTPUT_PATH}"
)


print("=" * 70)
print(
    "DATA PREPARATION COMPLETED SUCCESSFULLY"
)
print("=" * 70)
