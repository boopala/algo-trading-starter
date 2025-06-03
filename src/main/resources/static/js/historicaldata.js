// 1. When BackTest tab is clicked
function loadBackTestPanel() {
    console.log("loadBackTestPanel() called");
    fetch('/historical/init')
        .then(res => res.json())
        .then(data => {
        let html = `
                <form id="segmentExchangeForm">
                    <label>Segment:
                        <select id="segmentSelect">
                            ${data.segments.map(s => `<option value="${s}">${s}</option>`).join('')}
                        </select>
                    </label>
                    <label>Exchange:
                        <select id="exchangeSelect">
                            ${data.exchanges.map(e => `<option value="${e}">${e}</option>`).join('')}
                        </select>
                    </label>
                    <button type="submit">Submit</button>
                </form>
                <div id="equitiesSection"></div>
            `;
        document.getElementById('backTestPanel').innerHTML = html;
        document.getElementById('segmentExchangeForm').onsubmit = loadEquities;
    });
}

// 2. When segment/exchange is submitted
function loadEquities(event) {
    event.preventDefault();
    const segment = document.getElementById('segmentSelect').value;
    const exchange = document.getElementById('exchangeSelect').value;
    fetchEquities(segment, exchange, '', 0, 50);
}

function fetchEquities(segment, exchange, search, page, size) {
    fetch(`/historical/equities?segment=${segment}&exchange=${exchange}&search=${search}&page=${page}&size=${size}`)
        .then(res => res.json())
        .then(pageData => {
        renderEquitiesTable(pageData, segment, exchange, search, page, size);
    });
}

function renderEquitiesTable(pageData, segment, exchange, search, page, size) {
    let html = `
        <input type="text" id="searchEquity" placeholder="Search..." value="${search || ''}">
        <button onclick="fetchEquities('${segment}', '${exchange}', document.getElementById('searchEquity').value, 0, ${size})">Search</button>
        <button onclick="fetchEquities('${segment}', '${exchange}', '${search}', 0, ${size + 25})">+25</button>

        <table>
            <thead>
                <tr>
                    <th>Select</th>
                    <th>Equity Name</th>
                    <th>Segment</th>
                    <th>Exchange</th>
                </tr>
            </thead>
            <tbody>
                ${pageData.content.map(eq => `
                    <tr>
                        <td><input type="radio" name="selectedEquity" value="${eq.equityId}"></td>
                        <td>${eq.equityName?.name || 'N/A'}</td>
                        <td>${eq.segment?.name || 'N/A'}</td>
                        <td>${eq.exchange?.name || 'N/A'}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>

        <div>
            Page: ${page + 1} / ${pageData.totalPages}
            <button ${page === 0 ? 'disabled' : ''} onclick="fetchEquities('${segment}', '${exchange}', '${search}', ${page - 1}, ${size})">Prev</button>
            <button ${page === pageData.totalPages - 1 ? 'disabled' : ''} onclick="fetchEquities('${segment}', '${exchange}', '${search}', ${page + 1}, ${size})">Next</button>
        </div>

        <div>
            <label>Start Date: <input type="date" id="startDate"></label>
            <label>End Date: <input type="date" id="endDate"></label>
            <button onclick="submitEquity()">Submit Equity</button>
        </div>
    `;

    document.getElementById('equitiesSection').innerHTML = html;
}


function submitEquity() {
    const selected = document.querySelector('input[name="selectedEquity"]:checked');
    if (!selected) {
        alert('Please select an equity.');
        return;
    }
    const equityName = selected.value;
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    if (!startDate || !endDate) {
        alert('Please select start and end dates.');
        return;
    }
    fetch('/historical/data', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: `equityName=${encodeURIComponent(equityName)}&startDate=${startDate}&endDate=${endDate}`
    })
        .then(res => res.json())
        .then(data => {
        // Render historical data as needed
        alert('Historical data fetched! (Implement rendering as needed)');
    });
}
