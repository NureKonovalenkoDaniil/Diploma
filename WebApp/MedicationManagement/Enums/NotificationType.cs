namespace MedicationManagement.Enums
{
    public enum NotificationType
    {
        Expiry,             // Наближення терміну придатності
        LowStock,           // Низький запас
        StorageViolation,   // Порушення умов зберігання
        StorageRestored,    // Умови зберігання нормалізовані
        IncidentCreated     // Новий інцидент зберігання
    }
}
