addEventListener('DOMContentLoaded', async () => {
  const medicine = document.getElementById('medicineDetails');

  const token = localStorage.getItem('token');
  if (!token) {
    window.location.href = 'login.html';
    return;
  }

  const urlParams = new URLSearchParams(window.location.search);
  const medicineId = urlParams.get('id');

  if (!medicineId) {
    medicine.innerHTML = `<div class="alert alert-danger">No medicine ID provided.</div>`;
    return;
  }

  try {
    const response = await fetch(`/api/medicine/${medicineId}`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error('Medicine not found or unauthorized access.');
    }

    const med = await response.json();
    medicine.innerHTML = `
        <h4 class="card-title text-primary mb-3">${med.name}</h4>
        <ul class="list-group list-group-flush">
        <li class="list-group-item"><strong>Type:</strong> ${med.type}</li>
        <li class="list-group-item"><strong>Expiry Date:</strong> ${new Date(
          med.expiryDate,
        ).toLocaleDateString()}</li>
        <li class="list-group-item"><strong>Quantity:</strong> ${med.quantity}</li>
        <li class="list-group-item"><strong>Category:</strong> ${med.category}</li>
     </ul>
    `;
  } catch (error) {
    medicine.innerHTML = `<div class="alert alert-danger">Error loading medicine details: ${error.message}</div>`;
  }
});
