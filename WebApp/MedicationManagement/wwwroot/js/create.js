addEventListener('DOMContentLoaded', async () => {
  const form = document.getElementById('medicineForm');
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

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const medicine = {
      name: form.name.value,
      type: form.type.value,
      expiryDate: form.expiryDate.value,
      quantity: form.quantity.value,
      category: form.category.value,
    };

    try {
      const response = await fetch('/api/medicine', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(medicine),
      });

      if (response.ok) {
        messageBox.innerHTML = `<div class="alert alert-success">Medicine added successfully!</div>`;
        form.reset();
      } else {
        const text = await response.text();
        messageBox.innerHTML = `<div class="alert alert-danger">Error: ${text}</div>`;
      }
    } catch (error) {
      messageBox.innerHTML = `<div class="alert alert-danger">Error: ${error.message}</div>`;
    }
  });
});
