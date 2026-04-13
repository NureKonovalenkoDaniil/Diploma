addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('editForm');
  const messageBox = document.getElementById('message');
  const logoutBtn = document.getElementById('logoutBtn');

  const token = localStorage.getItem('token');
  if (!token) {
    window.location.href = 'login.html';
    return;
  }

  logoutBtn.addEventListener('click', () => {
    localStorage.removeItem('token');
    window.location.href = 'login.html';
  });

  const urlParams = new URLSearchParams(window.location.search);
  const medicineId = urlParams.get('id');

  if (!medicineId) {
    messageBox.innerHTML = `<div class="alert alert-danger">No medicine ID provided.</div>`;
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
    form.name.value = med.name;
    form.type.value = med.type;
    form.expiryDate.value = new Date(med.expiryDate).toISOString().split('T')[0];
    form.quantity.value = med.quantity;
    form.category.value = med.category;
  } catch (error) {
    messageBox.innerHTML = `<div class="alert alert-danger">Error loading medicine details: ${error.message}</div>`;
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const patchDoc = [
      { op: 'replace', path: '/name', value: document.getElementById('name').value },
      { op: 'replace', path: '/type', value: document.getElementById('type').value },
      { op: 'replace', path: '/expiryDate', value: document.getElementById('expiryDate').value },
      {
        op: 'replace',
        path: '/quantity',
        value: parseInt(document.getElementById('quantity').value),
      },
      { op: 'replace', path: '/category', value: document.getElementById('category').value },
    ];

    try {
      const response = await fetch(`/api/medicine/${medicineId}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(patchDoc),
      });

      if (response.ok) {
        messageBox.innerHTML = `<div class="alert alert-success">Medicine updated successfully!</div>`;
      } else {
        const text = await response.text();
        messageBox.innerHTML = `<div class="alert alert-danger">Error: ${text}</div>`;
      }
    } catch (error) {
      messageBox.innerHTML = `<div class="alert alert-danger">Error: ${error.message}</div>`;
    }
  });
});
