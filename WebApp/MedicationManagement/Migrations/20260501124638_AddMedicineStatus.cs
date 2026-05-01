using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace MedicationManagement.Migrations
{
    /// <inheritdoc />
    public partial class AddMedicineStatus : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "Status",
                table: "Medicines",
                type: "nvarchar(20)",
                maxLength: 20,
                nullable: false,
                defaultValue: "Active");

            // Backfill for existing rows (if any) that might end up with empty string.
            migrationBuilder.Sql("UPDATE [Medicines] SET [Status] = 'Active' WHERE [Status] = '' OR [Status] IS NULL");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Status",
                table: "Medicines");
        }
    }
}
