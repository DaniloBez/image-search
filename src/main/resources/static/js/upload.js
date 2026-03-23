const dropZone = document.getElementById('dropZone');
const fileInput = document.getElementById('fileInput');
const uploadList = document.getElementById('uploadList');

let totalFiles = 0;
let processedFiles = 0;

dropZone.onclick = (e) => {
    if (e.target !== fileInput) {
        fileInput.click();
    }
};

['dragover', 'dragenter'].forEach(type => {
    dropZone.addEventListener(type, (e) => { e.preventDefault(); dropZone.classList.add('over'); });
});

['dragleave', 'drop'].forEach(type => {
    dropZone.addEventListener(type, (e) => { e.preventDefault(); dropZone.classList.remove('over'); });
});

dropZone.addEventListener('drop', (e) => {
    handleUploads(e.dataTransfer.files).catch(err => console.error(err));
});

fileInput.onchange = (e) => {
    handleUploads(e.target.files).catch(err => console.error(err));
};

async function handleUploads(files) {
    if (files.length > 0) {
        document.getElementById('actionsArea').style.display = 'block';

        const rebuildBtn = document.getElementById('rebuildBtn');
        rebuildBtn.disabled = true;
        rebuildBtn.style.opacity = '0.6';
        rebuildBtn.style.cursor = 'not-allowed';
        rebuildBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Зачекайте, йде аналіз ШІ...';
    }

    const uploadTasks = [];
    Array.from(files).forEach(file => {
        totalFiles++;
        const item = createUploadItem(file);
        uploadList.appendChild(item);
        uploadTasks.push({ file, item });
    });

    for (const task of uploadTasks) {
        await performUpload(task.file, task.item);
    }
}

function performUpload(file, element) {
    return new Promise((resolve) => {
        const formData = new FormData();
        formData.append('file', file);

        const xhr = new XMLHttpRequest();
        xhr.open('POST', '/api/images/upload', true);

        const bar = element.querySelector('.progress-fill');
        const statusText = element.querySelector('.status');

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                const percent = (e.loaded / e.total) * 50;
                bar.style.width = percent + '%';
                statusText.innerText = "Відправка...";
            }
        };

        xhr.upload.onload = () => {
            bar.style.width = '75%';
            statusText.innerText = "Аналіз ШІ (зачекайте)...";
        };

        xhr.onload = () => {
            processedFiles++;
            if (xhr.status === 200) {
                bar.style.width = '100%';
                statusText.innerHTML = "Готово";
                statusText.style.color = "#4ade80";
            } else {
                bar.style.width = '100%';
                bar.style.background = '#ef4444';
                statusText.innerHTML = "Помилка AI";
                statusText.style.color = "#ef4444";
            }
            checkIfAllDone();
            resolve();
        };

        xhr.onerror = () => {
            processedFiles++;
            statusText.innerHTML = "Мережева помилка";
            statusText.style.color = "#ef4444";
            checkIfAllDone();
            resolve();
        };

        xhr.send(formData);
    });
}

function createUploadItem(file) {
    const div = document.createElement('div');
    div.className = 'upload-item';
    div.innerHTML = `
        <div style="display:flex; justify-content:space-between; margin-bottom:5px">
            <span style="font-weight: 600; color: var(--text);">${file.name}</span>
            <span class="status" style="color: var(--text-muted);">Очікування черги...</span>
        </div>
        <div class="progress-bg"><div class="progress-fill"></div></div>
    `;
    return div;
}

function checkIfAllDone() {
    if (processedFiles === totalFiles && totalFiles > 0) {
        showToast(`Успішно оброблено файлів: ${processedFiles}.`);

        const rebuildBtn = document.getElementById('rebuildBtn');
        rebuildBtn.disabled = false;
        rebuildBtn.style.opacity = '1';
        rebuildBtn.style.cursor = 'pointer';
        rebuildBtn.innerHTML = '<i class="fas fa-sync-alt" aria-hidden="true"></i> Зберегти та оновити індекс';

        totalFiles = 0;
        processedFiles = 0;
    }
}

function showToast(message) {
    let toast = document.getElementById("toast");
    if (!toast) {
        toast = document.createElement("div");
        toast.id = "toast";
        toast.className = "toast";
        document.body.appendChild(toast);
    }
    toast.innerHTML = message;
    toast.classList.add("show");

    setTimeout(() => { toast.classList.remove("show"); }, 4000);
}

async function startRebuild() {
    const btn = document.getElementById('rebuildBtn');
    const originalText = btn.innerHTML;

    btn.disabled = true;
    btn.style.opacity = '0.6';
    btn.style.cursor = 'not-allowed';
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Створення індексу TF-IDF...';

    try {
        const response = await fetch('/api/search/rebuild', { method: 'POST' });
        if (response.ok) {
            showToast("Індексація завершена!");
            setTimeout(() => window.location.href = "/", 1500);
        } else {
            showToast("Помилка при оновленні індексу.");
            btn.disabled = false;
            btn.style.opacity = '1';
            btn.style.cursor = 'pointer';
            btn.innerHTML = originalText;
        }
    } catch (err) {
        showToast("Помилка зв'язку з сервером.");
        btn.disabled = false;
        btn.style.opacity = '1';
        btn.style.cursor = 'pointer';
        btn.innerHTML = originalText;
    }
}