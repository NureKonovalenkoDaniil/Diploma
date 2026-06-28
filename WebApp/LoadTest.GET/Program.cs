using NBomber.CSharp;

using var httpClient = new HttpClient
{
    BaseAddress = new Uri("http://localhost:5001")
};

httpClient.DefaultRequestHeaders.Authorization =
    new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1laWQiOiJiYzA5YjNhOC00YzVhLTQ1YzUtOWVlOS03NjFhNmU4MDc0NmEiLCJ1bmlxdWVfbmFtZSI6ImFkbWluQG1lZHN0b3JhZ2UuY29tIiwiZW1haWwiOiJhZG1pbkBtZWRzdG9yYWdlLmNvbSIsInJvbGUiOiJBZG1pbmlzdHJhdG9yIiwiT3JnYW5pemF0aW9uSWQiOiJhOTVlNDYyYS1lYTA3LTQ5NzAtODZjMi0yOTExY2M1MzA4MzEiLCJuYmYiOjE3ODIxMzYzNjgsImV4cCI6MTc4NDcyODM2OCwiaWF0IjoxNzgyMTM2MzY4LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjUwMDEiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjUwMDEifQ.8sRU8P5lHcGVx31qGXpl5y-vTc0zgbkfanecT_BQydQ");

var scenario = Scenario.Create("GET /api/medicine", async context =>
{
    var response = await httpClient.GetAsync("/api/medicine");

    return response.IsSuccessStatusCode
        ? Response.Ok()
        : Response.Fail();
})
.WithWarmUpDuration(TimeSpan.FromSeconds(5))
.WithLoadSimulations(Simulation.KeepConstant(copies: 50, during: TimeSpan.FromSeconds(15)));

NBomberRunner
    .RegisterScenarios(scenario)
    .Run();
