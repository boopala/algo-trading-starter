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
        //loadHoldingsPanel();
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

let stompClient = null;

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        stompClient.subscribe('/topic/holdings', function (message) {
            const holdings = JSON.parse(message.body);
            updateHoldingsTable(holdings);
        });
    });
}

function updateHoldingsTable(holdings) {
    let totalInvested = 0, totalCurrentValue = 0, totalPnl = 0, totalDayChange = 0;
    let html = '';

    if (!holdings || holdings.length === 0) {
        html += `<tr><td colspan="8">No holdings available</td></tr>`;
    } else {
        holdings.sort((a, b) =>
        (a.tradingSymbol || '').localeCompare(b.tradingSymbol || '', undefined, { sensitivity: 'base' })
        );
        holdings.forEach(h => {
            const invested = h.quantity * h.averagePrice;
            const current = h.quantity * h.lastPrice;
            const pnl = h.pnl != null ? h.pnl : (current - invested);
            const dayChange = h.quantity * h.dayChange;
            totalInvested += invested;
            totalCurrentValue += current;
            totalPnl += pnl;
            totalDayChange += dayChange;

            html += `<tr>
                <td>${h.tradingSymbol}</td>
                <td>${h.quantity}</td>
                <td>${h.averagePrice != null ? h.averagePrice.toFixed(2) : '0.00'}</td>
                <td>${h.lastPrice}</td>
                <td>${invested.toFixed(2)}</td>
                <td>${current.toFixed(2)}</td>
                <td class="${pnl >= 0 ? 'profit' : 'loss'}">${pnl.toFixed(2)}</td>
                <td class="${dayChange >= 0 ? 'profit' : 'loss'}">${dayChange.toFixed(2)}</td>
            </tr>`;
        });

        // Add total row
        html += `<tr class="total-row">
            <td colspan="4" style="text-align: right;"><strong>Total:</strong></td>
            <td>₹${totalInvested.toFixed(2)}</td>
            <td>₹${totalCurrentValue.toFixed(2)}</td>
            <td class="${totalPnl >= 0 ? 'profit' : 'loss'}">${totalPnl.toFixed(2)}</td>
            <td class="${totalDayChange >= 0 ? 'profit' : 'loss'}">${totalDayChange.toFixed(2)}</td>
        </tr>`;
    }

    document.querySelector('#holdingsPanel tbody').innerHTML = html;
}


document.addEventListener("DOMContentLoaded", connectWebSocket);