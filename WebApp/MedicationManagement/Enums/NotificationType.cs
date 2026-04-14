namespace MedicationManagement.Enums
{
    public enum NotificationType
    {
        Expiry,             // Наближення терміну придатності
        LowStock,           // Низький запас
        StorageViolation,   // Порушення умов зберігання
        IncidentCreated     // Новий інцидент зберігання
    }
}
