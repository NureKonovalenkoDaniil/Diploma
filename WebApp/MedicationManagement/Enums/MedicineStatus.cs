namespace MedicationManagement.Enums
{
    /// <summary>
    /// Статус препарату в системі.
    /// Використовується для узгодженості між фактичним станом (запаси/термін) та lifecycle-подіями.
    /// </summary>
    public enum MedicineStatus
    {
        Active,
        Expired,
        Disposed,
        Recalled
    }
}

