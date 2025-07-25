class ZipDownloader {

    static download(includeJson) {
        fetch("/upload/get-uuid")
            .then(response => response.text())
            .then(uuid => {
                if (!uuid) {
                    alert("❌ UUID를 가져올 수 없습니다. 다시 시도해주세요.");
                    return;
                }

                let downloadUrl = includeJson
                    ? `/download/zip?uuid=${uuid}`
                    : `/download/zip/pdf-only?uuid=${uuid}`;

                let downloadButton = document.getElementById("downloadButton");
                let loadingOverlay = document.getElementById("loadingOverlay");
                let loadingMessage = document.getElementById("loadingMessage");

                downloadButton.disabled = true;
                loadingMessage.innerText = "다운로드 준비 중...";
                loadingOverlay.style.display = "flex";

                let xhr = new XMLHttpRequest();
                xhr.open("GET", downloadUrl, true);
                xhr.responseType = "blob";

                xhr.onprogress = function (event) {
                    if (event.lengthComputable) {
                        let percentComplete = Math.round((event.loaded / event.total) * 100);
                        loadingMessage.innerText = `다운로드 중... ${percentComplete}%`;
                    }
                };

                xhr.onload = function () {
                    if (xhr.status === 200) {
                        let contentDisposition = xhr.getResponseHeader("Content-Disposition");
                        let fileName = "ocr_results.zip";  // 기본 파일명

                        // 서버에서 전달한 파일명 가져오기
                        if (contentDisposition) {
                            let match = contentDisposition.match(/filename="(.+?)"/);
                            if (match) {
                                fileName = match[1];
                            }
                        }

                        let blob = xhr.response;
                        let url = window.URL.createObjectURL(blob);
                        let a = document.createElement("a");
                        a.href = url;
                        a.download = fileName;  // 파일명 적용
                        document.body.appendChild(a);
                        a.click();
                        document.body.removeChild(a);

                        ZipDownloader.showDownloadMessage();
                    } else {
                        loadingMessage.innerText = "❌ 다운로드 실패!";
                    }
                };

                xhr.send();
            })
            .catch(error => {
                alert("❌ UUID 요청 중 오류 발생!");
                console.error(error);
            });
    }

    static showDownloadMessage() {
        let loadingOverlay = document.getElementById("loadingOverlay");
        let loadingMessage = document.getElementById("loadingMessage");

        loadingMessage.innerText = "다운로드 완료! 3초 후 메인 페이지로 이동합니다.";

        setTimeout(() => {
            loadingOverlay.style.display = "none";
            window.location.href = "/";
        }, 3000);
    }

}

// 전역 변수로 등록
window.ZipDownloader = ZipDownloader;