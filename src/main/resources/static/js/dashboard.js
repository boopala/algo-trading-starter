function loadHoldingsPanel() {
    console.log("Loading holdings...")
    fetch('/holdings')
        .then(res => res.text())
        .then(html => {
        document.getElementById('holdingsPanel').innerHTML = html;
    });
}

function loadPositionsPanel() {
    fetch('/positions')
        .then(res => res.text())
        .then(html => {
        document.getElementById('positionsPanel').innerHTML = html;
    });
}

function showPanel(panel) {
    // Tabs
    document.getElementById('holdingsTab').classList.remove('active');
    document.getElementById('positionsTab').classList.remove('active');
    document.getElementById('backTestTab').classList.remove('active');

    // Panels
    document.getElementById('holdingsPanel').classList.remove('active');
    document.getElementById('positionsPanel').classList.remove('active');
    document.getElementById('backTestPanel').classList.remove('active');
    if (panel === 'holdings') {
        document.getElementById('holdingsTab').classList.add('active');
        document.getElementById('holdingsPanel').classList.add('active');
        loadHoldingsPanel();
        //setInterval(loadHoldingsPanel, 10000);
    } else if (panel === 'positions') {
        document.getElementById('positionsTab').classList.add('active');
        document.getElementById('positionsPanel').classList.add('active');
        loadPositionsPanel();
    } else if (panel === 'backtest') {
        document.getElementById('backTestTab').classList.add('active');
        document.getElementById('backTestPanel').classList.add('active');
        console.log("BackTest tab clicked, calling loadBackTestPanel()");
        loadBackTestPanel();
    }
}
