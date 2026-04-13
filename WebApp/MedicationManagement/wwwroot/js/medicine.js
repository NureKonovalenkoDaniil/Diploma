document.addEventListener('DOMContentLoaded', async () => {
  const token = localStorage.getItem('token');

  if (!token) {
    window.location.href = 'login.html';
    return;
  }

  const tableBody = document.querySelector('#medicineTable tbody');

  try {
    const response = await fetch('/api/medicine', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error('Unathorized access or server error.');
    }

    const medicines = await response.json();

    medicines.forEach((med) => {
      const row = document.createElement('tr');
      row.innerHTML = `
        <td>${med.name}</td>
        <td>${med.type}</td>
        <td>${new Date(med.expiryDate).toLocaleDateString()}</td>
        <td>${med.quantity}</td>
        <td>${med.category}</td>
        <td>
        <a href="details.html?id=${med.medicineID}" class="btn btn-sm btn-info">Details</a>
        <a href="edit.html?id=${med.medicineID}" class="btn btn-sm btn-warning">Edit</a>
        <button class="btn btn-danger btn-sm me-0" onclick="confirmDelete(${
          med.medicineID
        })">Delete</button>
        </td>
      `;
      tableBody.appendChild(row);
    });
  } catch (error) {
    tableBody.innerHTML = `<tr><td colspan="5" class="text-danger text-center">Error loading data</td></tr>`;
  }

  window.confirmDelete = (medicineId) => {
    if (confirm('Are you sure you want to delete this medicine?')) {
      deleteMedicine(medicineId);
    }
  };

  window.deleteMedicine = async (medicineId) => {
    try {
      const res = await fetch(`/api/medicine/${medicineId}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (res.ok) {
        window.location.reload();
      } else {
        const errorText = await res.text();
        alert(`Error deleting medicine: ${errorText}`);
      }
    } catch (error) {
      alert(`Error deleting medicine: ${error.message}`);
    }
  };

  const logoutBtn = document.getElementById('logoutBtn');
  logoutBtn.addEventListener('click', () => {
    localStorage.removeItem('token');
    window.location.href = 'login.html';
  });

  document.getElementById('searchInput').addEventListener('input', function () {
    const query = this.value.toLowerCase();
    document.querySelectorAll('tbody tr').forEach((row) => {
      row.style.display = row.innerText.toLowerCase().includes(query) ? '' : 'none';
    });
  });
});
