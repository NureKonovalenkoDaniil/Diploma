document.addEventListener('DOMContentLoaded', () => {
  const token = localStorage.getItem('token');
  const box = document.getElementById('notifications');

  if (!token) {
    window.location.href = 'login.html';
    return;
  }

  async function loadNotifications() {
    box.innerHTML = '';

    try {
      const headers = { Authorization: `Bearer ${token}` };

      const [expiringRes, lowStockRes, replenishRes, conditionRes] = await Promise.all([
        fetch('/api/medicine/expiring?daysThreshold=7', { headers }),
        fetch('/api/medicine/low-stock?threshold=10', { headers }),
        fetch('/api/medicine/replenishment-recommendations', { headers }),
        fetch('/api/storagecondition/checkCondition', { headers }),
      ]);

      const expiring = await expiringRes.json();
      const lowStock = await lowStockRes.json();
      const replenish = await replenishRes.json();
      const violations = await conditionRes.json();

      if (expiring.length > 0) {
        box.innerHTML += `
          <div class="alert alert-warning">
            <strong class="d-flex justify-content-between align-items-center">
              Expiring Medicines (${expiring.length})
              <button class="btn btn-sm btn-outline-dark" type="button" data-bs-toggle="collapse" data-bs-target="#expiringCollapse">Toggle</button>
            </strong>
            <div class="collapse" id="expiringCollapse">
              <ul>${expiring
                .map(
                  (m) => `<li>${m.name} (exp: ${new Date(m.expiryDate).toLocaleDateString()})</li>`,
                )
                .join('')}</ul>
            </div>
          </div>`;
      }

      if (lowStock.length > 0) {
        box.innerHTML += `
          <div class="alert alert-danger">
            <strong class="d-flex justify-content-between align-items-center">
              Low Stock (${lowStock.length})
              <button class="btn btn-sm btn-outline-light" type="button" data-bs-toggle="collapse" data-bs-target="#lowStockCollapse">Toggle</button>
            </strong>
            <div class="collapse" id="lowStockCollapse">
              <ul>${lowStock.map((m) => `<li>${m.name} (Qty: ${m.quantity})</li>`).join('')}</ul>
            </div>
          </div>`;
      }

      if (replenish.length > 0) {
        box.innerHTML += `
          <div class="alert alert-info">
            <strong class="d-flex justify-content-between align-items-center">
              Replenishment Recommendations (${replenish.length})
              <button class="btn btn-sm btn-outline-dark" type="button" data-bs-toggle="collapse" data-bs-target="#replenishCollapse">Toggle</button>
            </strong>
            <div class="collapse" id="replenishCollapse">
              <ul>${replenish
                .map((r) => `<li>${r.medicineName} → ${r.recommendedQuantity}</li>`)
                .join('')}</ul>
            </div>
          </div>`;
      }

      if (violations.length > 0) {
        box.innerHTML += `
          <div class="alert alert-danger">
            <strong class="d-flex justify-content-between align-items-center">
              Storage Violations (${violations.length})
              <button class="btn btn-sm btn-outline-light" type="button" data-bs-toggle="collapse" data-bs-target="#violationCollapse">Toggle</button>
            </strong>
            <div class="collapse" id="violationCollapse">
              <ul>${violations.map((v) => `<li>${v}</li>`).join('')}</ul>
            </div>
          </div>`;
      }

      if (box.innerHTML.trim() === '') {
        box.innerHTML = `<div class="alert alert-success">No notifications at this time.</div>`;
      }
    } catch (err) {
      box.innerHTML = `<div class="alert alert-danger">Error loading notifications</div>`;
    }
  }

  loadNotifications();
  setInterval(loadNotifications, 30000); // 60 сек
});
