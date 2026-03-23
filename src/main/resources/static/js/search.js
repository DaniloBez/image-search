let currentImageId = null;

document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('imageModal');

    document.querySelector('.grid').addEventListener('click', (e) => {
        const card = e.target.closest('.card');
        if (card) openModal(card);
    });

    document.querySelector('.grid').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            const card = e.target.closest('.card');
            if (card) openModal(card);
        }
    });

    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });

    document.querySelector('.close-btn').addEventListener('click', closeModal);
});

function showToast(message, isError = false) {
    let toast = document.getElementById("toast");
    if (!toast) {
        toast = document.createElement("div");
        toast.id = "toast";
        toast.className = "toast";
        document.body.appendChild(toast);
    }

    toast.innerHTML = message;

    if (isError)
        toast.style.borderColor = "var(--danger)";
    else
        toast.style.borderColor = "var(--primary)";

    toast.classList.add("show");
    setTimeout(() => { toast.classList.remove("show"); }, 4000);
}

function getHighlightedText(originalText) {
    const urlParams = new URLSearchParams(window.location.search);
    const query = urlParams.get('q');

    if (!query || query.trim() === '' || !originalText)
        return originalText;

    const searchWords = query.trim()
        .replace(/[,.]/g, '')
        .split(/\s+/)
        .filter(word => word.length > 2);

    if (searchWords.length === 0) return originalText;

    const escapedWords = searchWords.map(word =>
        word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    );
    const pattern = `(${escapedWords.join('|')})`;
    const regex = new RegExp(pattern, 'gi');

    return originalText.replace(regex, '<mark>$1</mark>');
}

function openModal(card) {
    currentImageId = card.getAttribute('data-id');
    const modalImg = document.getElementById('modalImg');

    modalImg.src = card.getAttribute('data-url');
    document.getElementById('modalTitle').innerText = card.getAttribute('data-title');
    document.getElementById('modalScore').innerText = "Збіг: " + card.getAttribute('data-score');

    cancelEditMode();

    const originalDesc = card.getAttribute('data-desc') || "Опис недоступний.";
    document.getElementById('modalDesc').innerHTML = getHighlightedText(originalDesc);

    const modal = document.getElementById('imageModal');
    modal.classList.add('active');
    document.body.style.overflow = 'hidden';
}

window.closeModal = function() {
    const modal = document.getElementById('imageModal');
    const modalImg = document.getElementById('modalImg');

    modal.classList.remove('active');
    document.body.style.overflow = '';

    setTimeout(() => {
        modalImg.src = '';
    }, 200);
}

function enableEditMode() {
    document.getElementById('editDescInput').value = document.getElementById('modalDesc').innerText;

    document.getElementById('modalDesc').style.display = 'none';
    document.getElementById('modalActions').style.display = 'none';
    document.getElementById('editForm').style.display = 'flex';
}

function cancelEditMode() {
    document.getElementById('editForm').style.display = 'none';
    document.getElementById('modalDesc').style.display = 'block';
    document.getElementById('modalActions').style.display = 'flex';
}

async function saveDescription() {
    const newDesc = document.getElementById('editDescInput').value;
    const btn = document.querySelector('#editForm .btn-primary');
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Збереження...';
    btn.disabled = true;

    try {
        const response = await fetch(`/api/images/${currentImageId}/description`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ description: newDesc })
        });

        if (response.ok) {
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Оновлення індексу...';
            await fetch('/api/search/rebuild', { method: 'POST' });

            document.getElementById('modalDesc').innerText = newDesc;
            document.querySelector(`.card[data-id="${currentImageId}"]`).setAttribute('data-desc', newDesc);
            cancelEditMode();

            showToast('Опис та пошуковий індекс успішно оновлено!');
        } else
            showToast('Помилка при збереженні.', true);
    } catch (e) {
        showToast('Помилка мережі.', true);
    } finally {
        btn.innerHTML = '<i class="fas fa-save"></i> Зберегти';
        btn.disabled = false;
    }
}

async function deleteImage() {
    if (!confirm("Ви впевнені, що хочете назавжди видалити це зображення?")) return;

    try {
        const response = await fetch(`/api/images/${currentImageId}`, { method: 'DELETE' });

        if (response.ok || response.status === 204) {
            await fetch('/api/search/rebuild', { method: 'POST' });

            closeModal();
            const card = document.querySelector(`.card[data-id="${currentImageId}"]`);
            card.style.transform = 'scale(0)';
            card.style.opacity = '0';
            setTimeout(() => card.remove(), 300);

            showToast('✅ Зображення успішно видалено!');
        } else {
            showToast('Не вдалося видалити зображення.', true);
        }
    } catch (e) {
        showToast('Помилка мережі.', true);
    }
}