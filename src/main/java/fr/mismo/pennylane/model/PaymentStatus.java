package fr.mismo.pennylane.model;

public enum PaymentStatus {
    FULLY_PAID("fully_paid"),
    PARTIALLY_PAID("partially_paid"),
    TO_BE_PAID("to_be_paid"),
    TO_BE_SETTLED("to_be_settled"),
    OVERPAID("overpaid"),
    INVALID_AMOUNT("invalid_amount"),
    TO_BE_PROCESSED("to_be_processed");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PaymentStatus fromString(String text) {
        if (text == null) {
            return TO_BE_PROCESSED;
        }
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.value.equalsIgnoreCase(text)) {
                return status;
            }
        }
        return TO_BE_PROCESSED;
    }

    @Override
    public String toString() {
        return value;
    }
}
