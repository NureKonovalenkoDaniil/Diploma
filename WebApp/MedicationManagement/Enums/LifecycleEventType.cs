namespace MedicationManagement.Enums
{
    public enum LifecycleEventType
    {
        Received,   // Препарат надійшов на склад
        Issued,     // Препарат виданий
        Moved,      // Препарат переміщено між локаціями
        Expired,    // Термін придатності минув
        Disposed,   // Препарат утилізований
        Recalled    // Препарат відкликаний
    }
}
