// file-service 화면. 프레임워크 없이 fetch 만 씀.
// 서버 주소는 적지 않음 → 같은 출처(/api/...)로 보냄. ALB 뒤에서도 그대로 동작함.

const API = '/api/files';

const $ = (sel) => document.querySelector(sel);

function fmtSize(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1024 / 1024).toFixed(2) + ' MB';
}

function fmtTime(iso) {
  return new Date(iso).toLocaleString('ko-KR');
}

function showMessage(text, kind) {
  const el = $('#message');
  el.textContent = text;
  el.className = 'message ' + (kind || '');
}

async function loadStatus() {
  try {
    const res = await fetch('/api/status');
    const s = await res.json();
    const el = $('#status');
    el.textContent = `storage=${s.storage}  host=${s.host}  files=${s.files}`;
    el.className = 'status ' + s.storage;
  } catch (e) {
    $('#status').textContent = '상태 확인 실패';
  }
}

async function loadFiles() {
  const res = await fetch(API);
  const files = await res.json();
  const tbody = $('#files');
  tbody.innerHTML = '';
  if (files.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5">파일 없음</td></tr>';
    return;
  }
  for (const f of files) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${f.name}</td>
      <td>${fmtSize(f.size)}</td>
      <td>${fmtTime(f.uploadedAt)}</td>
      <td>
        <a class="link" href="${API}/${f.id}/content">앱 경유</a>
      </td>
      <td><button class="small danger" data-del="${f.id}">삭제</button></td>`;
    tbody.appendChild(tr);
  }
}

// 업로드
$('#upload-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const file = $('#file').files[0];
  if (!file) return;

  const form = new FormData();
  form.append('file', file);

  showMessage('업로드 중...');
  const res = await fetch(API, { method: 'POST', body: form });
  if (res.ok) {
    showMessage(`업로드 완료: ${file.name}`, 'ok');
    $('#upload-form').reset();
  } else {
    const err = await res.json().catch(() => ({ message: res.status }));
    showMessage(`실패 (${res.status}): ${err.message}`, 'error');
  }
  await Promise.all([loadFiles(), loadStatus()]);
});

// 목록 안의 버튼 · 링크 (이벤트 위임)
$('#files').addEventListener('click', async (ev) => {
  const t = ev.target;

  if (t.dataset.del) {
    if (!confirm('삭제할까요?')) return;
    const res = await fetch(`${API}/${t.dataset.del}`, { method: 'DELETE' });
    showMessage(res.ok ? '삭제됨' : `삭제 실패 (${res.status})`, res.ok ? 'ok' : 'error');
    await Promise.all([loadFiles(), loadStatus()]);
  }
});

$('#refresh').addEventListener('click', () => Promise.all([loadFiles(), loadStatus()]));

loadStatus();
loadFiles();
