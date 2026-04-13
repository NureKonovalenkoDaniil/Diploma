using NBomber.CSharp;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

var httpClient = new HttpClient
{
    BaseAddress = new Uri("https://localhost:7069")
};

httpClient.DefaultRequestHeaders.Authorization =
    new AuthenticationHeaderValue("Bearer", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1laWQiOiI2OGE2N2I1Mi1jZjc3LTQwY2ItYjk0Yy05MTAyYjlmOGZlOTYiLCJ1bmlxdWVfbmFtZSI6ImFkbWluQGdtYWlsLmNvbSIsImVtYWlsIjoiYWRtaW5AZ21haWwuY29tIiwicm9sZSI6IkFkbWluaXN0cmF0b3IiLCJuYmYiOjE3NDg2MTI1MDgsImV4cCI6MTc4MDE0ODUwOCwiaWF0IjoxNzQ4NjEyNTA4LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjcwNjkiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjcwNjkifQ.fQJRmaCU8TkjI3g63VCq68PSjBNtUVY43mLtmFd_FPI");

var random = new Random();

string GenerateMedicineJson()
{
    var medicine = new
    {
        Name = "TestMed_" + Guid.NewGuid().ToString("N").Substring(0, 6),
        Type = "Pill",
        ExpiryDate = DateTime.Now.AddDays(random.Next(30, 365)).ToString("yyyy-MM-dd"),
        Quantity = random.Next(1, 100),
        Category = "General"
    };

    return JsonSerializer.Serialize(medicine);
}

var scenario = Scenario.Create("POST /api/medicine", async context =>
{
    var json = GenerateMedicineJson();
    var content = new StringContent(json, Encoding.UTF8, "application/json");

    var response = await httpClient.PostAsync("/api/medicine", content);

    return response.IsSuccessStatusCode
        ? Response.Ok()
        : Response.Fail();
})
.WithWarmUpDuration(TimeSpan.FromSeconds(3))
.WithLoadSimulations(Simulation.KeepConstant(copies: 10, during: TimeSpan.FromSeconds(20)));

NBomberRunner
    .RegisterScenarios(scenario)
    .Run();
