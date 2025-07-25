class ProgressBar {
    constructor() {
        this.progressContainer = document.querySelector('.progress-wrap');
        this.progressBar = document.createElement('div');
        this.progressBar.style.width = '0%';
        this.progressBar.style.height = '20px';
        this.progressBar.style.backgroundColor = '#4caf50';
        this.progressBar.style.color = 'white';
        this.progressBar.style.textAlign = 'center';
        this.progressBar.innerText = '0%';
        if (this.progressContainer) {
            this.progressContainer.innerHTML = "";  // 기존 진행률 제거 후 다시 추가
            this.progressContainer.appendChild(this.progressBar);
        }
    }

    startProgress() {
        this.progressContainer.style.display = 'block';
        this.progressBar.style.width = '0%';
        this.progressBar.innerText = '0%';
    }

    updateProgress(percentage) {
        let roundedPercentage = Math.round(percentage);
        this.progressBar.style.width = roundedPercentage + '%';
        this.progressBar.innerText = roundedPercentage + '%';
    }

    completeProgress() {
        this.updateProgress(100);
        this.progressBar.style.width = '100%';
        this.progressBar.innerText = '100%';
        setTimeout(() => {
            this.progressContainer.style.display = 'none';
            this.resetProgress();
        }, 1000);
    }

    resetProgress() {
        this.progressBar.style.width = '0%';
        this.progressBar.innerText = '0%';
    }
}