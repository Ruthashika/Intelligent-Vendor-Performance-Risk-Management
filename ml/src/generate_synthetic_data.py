import os
import random
from datetime import date, timedelta

from sqlalchemy import create_engine, text
from sqlalchemy.engine import URL


# ============================================================
# CONFIGURATION
# ============================================================

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Read database settings from environment variables. Do not place
# real usernames or passwords directly in this source file.
DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "3306")
DB_NAME = os.getenv("DB_NAME", "vendor_management_db")

if not DB_USER or not DB_PASSWORD:
    raise RuntimeError(
        "DB_USER and DB_PASSWORD environment variables must be set."
    )

# URL.create safely handles special characters in the password.
DATABASE_URL = URL.create(
    drivername="mysql+pymysql",
    username=DB_USER,
    password=DB_PASSWORD,
    host=DB_HOST,
    port=int(DB_PORT),
    database=DB_NAME,
)

engine = create_engine(DATABASE_URL)

# Fixed seed makes the synthetic dataset reproducible.
random.seed(42)


# ============================================================
# SAFETY CHECK
# ============================================================

print("=" * 70)
print("REALISTIC SYNTHETIC DATA GENERATOR")
print("=" * 70)

with engine.connect() as conn:

    po_count = conn.execute(
        text("SELECT COUNT(*) FROM purchase_orders")
    ).scalar()

    vendor_count = conn.execute(
        text("SELECT COUNT(*) FROM vendors")
    ).scalar()

    print(f"\nExisting purchase orders : {po_count}")
    print(f"Existing vendors         : {vendor_count}")

    # We intentionally expect the current development database
    # to contain the previous 200-record dataset.
    if po_count != 200:
        raise RuntimeError(
            "\nSafety check failed.\n"
            "Expected exactly 200 existing purchase orders "
            "before replacing the development dataset.\n"
            f"Found {po_count}.\n\n"
            "The script will NOT modify the database."
        )

    if vendor_count != 10:
        raise RuntimeError(
            "\nSafety check failed.\n"
            "Expected exactly 10 vendors.\n"
            f"Found {vendor_count}."
        )

print("\nSafety check passed.")
print("Current 200-record dataset will be replaced.")
print("New dataset target: 500 purchase orders.")


# ============================================================
# LOAD VENDORS
# ============================================================

with engine.connect() as conn:

    vendors = conn.execute(
        text("""
            SELECT id, vendor_name, category
            FROM vendors
            WHERE id BETWEEN 2 AND 11
            ORDER BY id
        """)
    ).fetchall()

if len(vendors) != 10:
    raise RuntimeError(
        f"Expected 10 synthetic vendors, found {len(vendors)}."
    )


# ============================================================
# VENDOR RELIABILITY PROFILES
# ============================================================
#
# These are simulation parameters, NOT ML labels.
#
# A vendor's historical reliability influences the probability
# of future delivery problems.
#
# This creates a realistic temporal relationship:
#
# Previous performance
#        ↓
# Future delivery probability
#        ↓
# Actual simulated outcome
#        ↓
# Delivery Risk label
#
# ============================================================

vendor_profiles = {

    2: {
        "risk_level": "low",
        "late_probability": 0.12,
        "partial_probability": 0.04,
        "quality_probability": 0.05
    },

    3: {
        "risk_level": "low",
        "late_probability": 0.10,
        "partial_probability": 0.03,
        "quality_probability": 0.06
    },

    4: {
        "risk_level": "low",
        "late_probability": 0.08,
        "partial_probability": 0.03,
        "quality_probability": 0.04
    },

    5: {
        "risk_level": "low",
        "late_probability": 0.15,
        "partial_probability": 0.05,
        "quality_probability": 0.08
    },

    6: {
        "risk_level": "medium",
        "late_probability": 0.25,
        "partial_probability": 0.10,
        "quality_probability": 0.12
    },

    7: {
        "risk_level": "medium",
        "late_probability": 0.30,
        "partial_probability": 0.12,
        "quality_probability": 0.15
    },

    8: {
        "risk_level": "high",
        "late_probability": 0.45,
        "partial_probability": 0.20,
        "quality_probability": 0.25
    },

    9: {
        "risk_level": "high",
        "late_probability": 0.48,
        "partial_probability": 0.22,
        "quality_probability": 0.28
    },

    10: {
        "risk_level": "medium",
        "late_probability": 0.32,
        "partial_probability": 0.14,
        "quality_probability": 0.18
    },

    11: {
        "risk_level": "medium",
        "late_probability": 0.28,
        "partial_probability": 0.12,
        "quality_probability": 0.16
    }
}


# ============================================================
# CLEAR SYNTHETIC DEVELOPMENT DATA
# ============================================================

with engine.begin() as conn:

    print("\nDeleting previous synthetic development records...")

    # Child tables first because of foreign keys.
    conn.execute(text("DELETE FROM alerts"))
    conn.execute(text("DELETE FROM vendor_feedback"))
    conn.execute(text("DELETE FROM quality_evaluations"))
    conn.execute(text("DELETE FROM purchase_orders"))

    # Rebuild performance summaries after new data.
    conn.execute(text("DELETE FROM vendor_performance_scores"))

    print("Old development records removed.")


# ============================================================
# GENERATION PARAMETERS
# ============================================================

# 10 vendors × 50 POs = 500 POs
TOTAL_PER_VENDOR = 50

START_DATE = date(2026, 9, 1)

prior_vendor_history = {
    vendor_id: []
    for vendor_id, _, _ in vendors
}


# ============================================================
# HELPER FUNCTIONS
# ============================================================

def clamp(value, minimum, maximum):
    return max(minimum, min(maximum, value))


def calculate_history(vendor_history):

    if not vendor_history:

        return {
            "previous_po_count": 0,
            "previous_on_time_rate": 0.0,
            "previous_average_delay": 0.0,
            "previous_fulfillment_rate": 100.0,
            "previous_quality_issue_rate": 0.0
        }

    count = len(vendor_history)

    on_time_count = sum(
        1
        for item in vendor_history
        if item["delay"] <= 0
    )

    average_delay = sum(
        item["delay"]
        for item in vendor_history
    ) / count

    average_fulfillment = sum(
        item["fulfillment"]
        for item in vendor_history
    ) / count

    quality_issue_rate = (
        sum(
            1
            for item in vendor_history
            if item["quality_issue"] > 0
        )
        / count
    ) * 100

    return {
        "previous_po_count": count,

        "previous_on_time_rate": (
            on_time_count / count
        ) * 100,

        "previous_average_delay": average_delay,

        "previous_fulfillment_rate": average_fulfillment,

        "previous_quality_issue_rate": quality_issue_rate
    }


# ============================================================
# GENERATE PURCHASE ORDERS
# ============================================================

print("\nGenerating realistic purchase-order history...")

print("Target:")
print(" - Vendors : 10")
print(" - POs per vendor : 50")
print(" - Total POs : 500")


all_generated = []


with engine.begin() as conn:

    po_number_counter = 1

    for vendor_id, vendor_name, category in vendors:

        profile = vendor_profiles[vendor_id]

        print(
            f"\nGenerating POs for "
            f"{vendor_name} (Vendor ID {vendor_id})..."
        )

        for history_number in range(TOTAL_PER_VENDOR):

            # ------------------------------------------------
            # ORDER DATE
            # ------------------------------------------------

            order_date = (
                START_DATE
                + timedelta(
                    days=history_number * 4
                    + random.randint(0, 2)
                    + (vendor_id - 2)
                )
            )

            # ------------------------------------------------
            # HISTORICAL FEATURES
            # ------------------------------------------------

            history = prior_vendor_history[vendor_id]

            historical = calculate_history(history)

            # ------------------------------------------------
            # PURCHASE ORDER CHARACTERISTICS
            # ------------------------------------------------

            priority = random.choices(
                ["LOW", "MEDIUM", "HIGH"],
                weights=[30, 50, 20]
            )[0]

            quantity = random.randint(50, 500)

            unit_price = random.choice(
                [
                    100,
                    150,
                    200,
                    250,
                    300,
                    400,
                    500,
                    750,
                    1000
                ]
            )

            total_amount = quantity * unit_price

            planned_delivery_days = random.choice(
                [5, 7, 10, 12, 14]
            )

            expected_date = (
                order_date
                + timedelta(days=planned_delivery_days)
            )

            # ------------------------------------------------
            # RISK MODIFIERS
            # ------------------------------------------------

            late_probability = profile["late_probability"]

            partial_probability = profile[
                "partial_probability"
            ]

            # Historical performance influences future risk.
            if history:

                if historical["previous_on_time_rate"] < 60:
                    late_probability += 0.12

                elif historical["previous_on_time_rate"] < 80:
                    late_probability += 0.06

                if historical["previous_average_delay"] > 2:
                    late_probability += 0.08

                elif historical["previous_average_delay"] > 1:
                    late_probability += 0.04

                if historical["previous_fulfillment_rate"] < 95:
                    partial_probability += 0.08

                if historical["previous_quality_issue_rate"] > 30:
                    late_probability += 0.03

            # Priority has a small effect.
            if priority == "HIGH":
                late_probability += 0.03

            # Larger orders have slightly higher operational risk.
            if quantity > 400:
                late_probability += 0.03

            late_probability = clamp(
                late_probability,
                0.03,
                0.75
            )

            partial_probability = clamp(
                partial_probability,
                0.02,
                0.45
            )

            # ------------------------------------------------
            # SIMULATE DELIVERY OUTCOME
            # ------------------------------------------------

            late_event = (
                random.random()
                < late_probability
            )

            if late_event:

                delay = random.choices(
                    [-1, 0, 1, 2, 3, 4, 5],
                    weights=[
                        5,
                        10,
                        18,
                        25,
                        20,
                        14,
                        8
                    ]
                )[0]

            else:

                delay = random.choices(
                    [-4, -3, -2, -1, 0],
                    weights=[
                        10,
                        20,
                        30,
                        30,
                        10
                    ]
                )[0]

            actual_delivery_date = (
                expected_date
                + timedelta(days=delay)
            )

            # ------------------------------------------------
            # FULFILLMENT
            # ------------------------------------------------

            partial_event = (
                random.random()
                < partial_probability
            )

            if partial_event:

                fulfillment_percentage = random.randint(
                    75,
                    94
                )

                actual_quantity = int(
                    quantity
                    * fulfillment_percentage
                    / 100
                )

            else:

                actual_quantity = quantity

            fulfillment_rate = (
                actual_quantity / quantity
            ) * 100

            # ------------------------------------------------
            # QUALITY
            # ------------------------------------------------

            quality_event = (
                random.random()
                < profile["quality_probability"]
            )

            if quality_event:

                issue_count = random.choice(
                    [1, 1, 2]
                )

                quality_status = "ACCEPTABLE"

                severity = random.choice(
                    ["LOW", "MEDIUM"]
                )

                remarks = (
                    "Minor quality issue identified "
                    "during inspection."
                )

            else:

                issue_count = 0
                quality_status = "GOOD"
                severity = "LOW"

                remarks = (
                    "Materials received in good condition."
                )

            # ------------------------------------------------
            # PO NUMBER
            # ------------------------------------------------

            po_number = f"PO-{po_number_counter:03d}"

            po_number_counter += 1

            # ------------------------------------------------
            # INSERT PURCHASE ORDER
            # ------------------------------------------------

            result = conn.execute(
                text("""
                    INSERT INTO purchase_orders
                    (
                        po_number,
                        order_date,
                        expected_delivery_date,
                        actual_delivery_date,
                        priority,
                        quantity,
                        actual_delivered_quantity,
                        status,
                        unit_price,
                        total_amount,
                        vendor_id
                    )
                    VALUES
                    (
                        :po_number,
                        :order_date,
                        :expected_delivery_date,
                        :actual_delivery_date,
                        :priority,
                        :quantity,
                        :actual_delivered_quantity,
                        'DELIVERED',
                        :unit_price,
                        :total_amount,
                        :vendor_id
                    )
                """),
                {
                    "po_number": po_number,
                    "order_date": order_date,
                    "expected_delivery_date": expected_date,
                    "actual_delivery_date": actual_delivery_date,
                    "priority": priority,
                    "quantity": quantity,
                    "actual_delivered_quantity": actual_quantity,
                    "unit_price": unit_price,
                    "total_amount": total_amount,
                    "vendor_id": vendor_id
                }
            )

            po_id = result.lastrowid

            # ------------------------------------------------
            # QUALITY EVALUATION
            # ------------------------------------------------

            conn.execute(
                text("""
                    INSERT INTO quality_evaluations
                    (
                        evaluation_date,
                        issue_count,
                        quality_status,
                        remarks,
                        severity,
                        purchase_order_id
                    )
                    VALUES
                    (
                        :evaluation_date,
                        :issue_count,
                        :quality_status,
                        :remarks,
                        :severity,
                        :purchase_order_id
                    )
                """),
                {
                    "evaluation_date": actual_delivery_date,
                    "issue_count": issue_count,
                    "quality_status": quality_status,
                    "remarks": remarks,
                    "severity": severity,
                    "purchase_order_id": po_id
                }
            )

            # ------------------------------------------------
            # FEEDBACK
            # ------------------------------------------------

            if delay <= 0 and fulfillment_rate == 100:

                rating = 5

                feedback_text = (
                    "Delivery completed on time "
                    "with full quantity."
                )

            elif delay <= 2 and fulfillment_rate == 100:

                rating = 4

                feedback_text = (
                    "Delivery completed successfully "
                    "with a minor delay."
                )

            elif fulfillment_rate < 100:

                rating = 3

                feedback_text = (
                    "Delivery completed with partial "
                    "quantity received."
                )

            else:

                rating = 2

                feedback_text = (
                    "Delivery experienced a significant delay."
                )

            feedback_type = "DELIVERY"

            conn.execute(
                text("""
                    INSERT INTO vendor_feedback
                    (
                        feedback_text,
                        feedback_type,
                        rating,
                        submitted_at,
                        purchase_order_id,
                        vendor_id
                    )
                    VALUES
                    (
                        :feedback_text,
                        :feedback_type,
                        :rating,
                        :submitted_at,
                        :purchase_order_id,
                        :vendor_id
                    )
                """),
                {
                    "feedback_text": feedback_text,
                    "feedback_type": feedback_type,
                    "rating": rating,
                    "submitted_at": actual_delivery_date,
                    "purchase_order_id": po_id,
                    "vendor_id": vendor_id
                }
            )

            # ------------------------------------------------
            # ALERTS
            # ------------------------------------------------

            if delay >= 3:

                conn.execute(
                    text("""
                        INSERT INTO alerts
                        (
                            alert_type,
                            created_at,
                            message,
                            severity,
                            status,
                            purchase_order_id,
                            vendor_id
                        )
                        VALUES
                        (
                            'DELIVERY_DELAY',
                            :created_at,
                            :message,
                            'HIGH',
                            'OPEN',
                            :purchase_order_id,
                            :vendor_id
                        )
                    """),
                    {
                        "created_at": actual_delivery_date,
                        "message": (
                            f"Purchase order {po_number} "
                            "was delivered after the expected "
                            "delivery date."
                        ),
                        "purchase_order_id": po_id,
                        "vendor_id": vendor_id
                    }
                )

            elif fulfillment_rate < 90:

                conn.execute(
                    text("""
                        INSERT INTO alerts
                        (
                            alert_type,
                            created_at,
                            message,
                            severity,
                            status,
                            purchase_order_id,
                            vendor_id
                        )
                        VALUES
                        (
                            'PARTIAL_DELIVERY',
                            :created_at,
                            :message,
                            'MEDIUM',
                            'OPEN',
                            :purchase_order_id,
                            :vendor_id
                        )
                    """),
                    {
                        "created_at": actual_delivery_date,
                        "message": (
                            f"Purchase order {po_number} "
                            "was received with partial quantity."
                        ),
                        "purchase_order_id": po_id,
                        "vendor_id": vendor_id
                    }
                )

            # ------------------------------------------------
            # UPDATE VENDOR HISTORY
            # ------------------------------------------------

            prior_vendor_history[vendor_id].append(
                {
                    "delay": delay,
                    "fulfillment": fulfillment_rate,
                    "quality_issue": issue_count
                }
            )

            all_generated.append(
                {
                    "po_number": po_number,
                    "vendor_id": vendor_id,
                    "delay": delay,
                    "fulfillment": fulfillment_rate,
                    "quality_issue": issue_count
                }
            )


# ============================================================
# REBUILD VENDOR PERFORMANCE SCORES
# ============================================================

print("\nCalculating vendor performance scores...")


with engine.begin() as conn:

    for vendor_id, vendor_name, category in vendors:

        records = prior_vendor_history[vendor_id]

        count = len(records)

        avg_delay = sum(
            item["delay"]
            for item in records
        ) / count

        on_time_rate = (
            sum(
                1
                for item in records
                if item["delay"] <= 0
            )
            / count
        ) * 100

        fulfillment_rate = sum(
            item["fulfillment"]
            for item in records
        ) / count

        quality_issue_rate = (
            sum(
                1
                for item in records
                if item["quality_issue"] > 0
            )
            / count
        ) * 100

        quality_score = (
            100 - quality_issue_rate
        )

        if avg_delay <= 0:

            delivery_score = 100

        else:

            delivery_score = max(
                0,
                100 - avg_delay * 10
            )

        performance_score = (
            0.40 * on_time_rate
            + 0.30 * fulfillment_rate
            + 0.20 * quality_score
            + 0.10 * delivery_score
        )

        conn.execute(
            text("""
                INSERT INTO vendor_performance_scores
                (
                    average_delivery_delay,
                    calculated_at,
                    fulfillment_rate,
                    on_time_delivery_rate,
                    performance_score,
                    quality_issue_rate,
                    vendor_id
                )
                VALUES
                (
                    :average_delivery_delay,
                    NOW(),
                    :fulfillment_rate,
                    :on_time_delivery_rate,
                    :performance_score,
                    :quality_issue_rate,
                    :vendor_id
                )
            """),
            {
                "average_delivery_delay": round(
                    avg_delay,
                    2
                ),

                "fulfillment_rate": round(
                    fulfillment_rate,
                    2
                ),

                "on_time_delivery_rate": round(
                    on_time_rate,
                    2
                ),

                "performance_score": round(
                    performance_score,
                    2
                ),

                "quality_issue_rate": round(
                    quality_issue_rate,
                    2
                ),

                "vendor_id": vendor_id
            }
        )


# ============================================================
# FINAL VERIFICATION
# ============================================================

with engine.connect() as conn:

    po_count = conn.execute(
        text("SELECT COUNT(*) FROM purchase_orders")
    ).scalar()

    quality_count = conn.execute(
        text("SELECT COUNT(*) FROM quality_evaluations")
    ).scalar()

    feedback_count = conn.execute(
        text("SELECT COUNT(*) FROM vendor_feedback")
    ).scalar()

    performance_count = conn.execute(
        text("SELECT COUNT(*) FROM vendor_performance_scores")
    ).scalar()

    alert_count = conn.execute(
        text("SELECT COUNT(*) FROM alerts")
    ).scalar()


print("\n")
print("=" * 70)
print("SYNTHETIC DATA GENERATION COMPLETED")
print("=" * 70)

print(f"Purchase Orders       : {po_count}")
print(f"Quality Evaluations   : {quality_count}")
print(f"Vendor Feedback       : {feedback_count}")
print(f"Performance Scores    : {performance_count}")
print(f"Alerts                : {alert_count}")

# ============================================================
# 500-RECORD VALIDATION
# ============================================================

if (
    po_count == 500
    and quality_count == 500
    and feedback_count == 500
    and performance_count == 10
):

    print("\nAll record-count checks PASSED.")

else:

    raise RuntimeError(
        "\nRecord-count verification failed.\n"
        f"Expected:\n"
        f"  Purchase Orders     : 500\n"
        f"  Quality Evaluations : 500\n"
        f"  Vendor Feedback     : 500\n"
        f"  Performance Scores  : 10\n\n"
        f"Found:\n"
        f"  Purchase Orders     : {po_count}\n"
        f"  Quality Evaluations : {quality_count}\n"
        f"  Vendor Feedback     : {feedback_count}\n"
        f"  Performance Scores  : {performance_count}"
    )

print("=" * 70)
