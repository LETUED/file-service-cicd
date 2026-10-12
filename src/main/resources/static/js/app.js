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
        <a class="link" href="${API}/${f.id}/url" data-url="${f.id}">직접(URL)</a>
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

// 발급된 프리사인 URL 을 화면에 보여 줌. 호스트가 S3 이고 X-Amz-Expires 가 들어 있는 것을 눈으로 확인하는 용도
function showPresignedUrl(url, expiresIn) {
  let box = $('#url-box');
  if (!box) {
    box = document.createElement('div');
    box.id = 'url-box';
    box.className = 'url-box';
    $('#files').closest('section').appendChild(box);
  }
  const u = new URL(url);
  const params = u.searchParams;
  box.innerHTML = `
    <p class="url-title">발급된 프리사인 URL</p>
    <table class="url-meta">
      <tr><th>호스트</th><td>${u.host}</td></tr>
      <tr><th>객체 키</th><td>${decodeURIComponent(u.pathname.slice(1))}</td></tr>
      <tr><th>X-Amz-Expires</th><td>${params.get('X-Amz-Expires') || expiresIn} 초</td></tr>
      <tr><th>X-Amz-Date</th><td>${params.get('X-Amz-Date') || '-'}</td></tr>
    </table>
    <textarea class="url-text" readonly rows="3"></textarea>
    <div>
      <a class="link" href="${url}" target="_blank" rel="noopener">새 탭에서 열기</a>
      <button class="small" id="url-copy">주소 복사</button>
    </div>`;
  box.querySelector('.url-text').value = url;
  box.querySelector('#url-copy').addEventListener('click', async () => {
    try { await navigator.clipboard.writeText(url); showMessage('주소를 복사함', 'ok'); }
    catch (e) { box.querySelector('.url-text').select(); }
  });
}

// 목록 안의 버튼 · 링크 (이벤트 위임)
$('#files').addEventListener('click', async (ev) => {
  const t = ev.target;

  if (t.dataset.url) {
    // 1단계 : 앱에 프리사인 URL 발급 요청. 앱은 URL 만 서명하고 데이터 경로에서 빠짐
    ev.preventDefault();
    const res = await fetch(`${API}/${t.dataset.url}/url`);
    if (res.status === 409) {
      showMessage('디스크 저장소에서는 직접 다운로드 URL 을 만들 수 없음 (S3 저장소에서만 동작)', 'error');
      return;
    }
    if (res.status === 404) {
      // 목록이 인스턴스 메모리에 있어, 업로드를 받은 인스턴스가 아니면 이 id 를 모름 (인스턴스 2대 이상일 때)
      showMessage('404 · 이 요청을 받은 인스턴스의 목록에 없는 파일 (다른 인스턴스가 업로드를 받음). 새로 고침 뒤 다시 시도', 'error');
      return;
    }
    if (!res.ok) {
      showMessage(`URL 발급 실패 (${res.status})`, 'error');
      return;
    }
    const body = await res.json();
    showPresignedUrl(body.url, body.expiresIn);
    // 2단계 : 링크 자체를 S3 주소로 바꿈. 다시 클릭하면 브라우저가 S3 로 직접 감 (Network 탭에서 호스트 확인)
    t.href = body.url;
    t.target = '_blank';
    t.rel = 'noopener';
    t.textContent = `S3 에서 받기 (${body.expiresIn}초)`;
    t.classList.add('ready');
    delete t.dataset.url;
    showMessage(`임시 URL 발급 (유효 ${body.expiresIn}초). 아래 주소를 확인하고 "S3 에서 받기" 를 클릭`, 'ok');
  }

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
