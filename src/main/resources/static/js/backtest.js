let currentPage = 0;
let pageSize = 50;
let currentSegment = '';
let currentExchange = '';
let currentSearch = '';

function fetchEquities(segment, exchange, search, page, size) {
    currentSegment = segment;
    currentExchange = exchange;
    currentSearch = search;
    currentPage = page;
    pageSize = size;

    fetch(`/historical/equities?segment=${segment}&exchange=${exchange}&search=${search}&page=${page}&size=${size}`)
        .then(res => res.json())
        .then(pageData => renderEquitiesTable(pageData))
        .catch(error => console.error(error));
}

function renderEquitiesTable(pageData) {
    document.getElementById('equitiesSection').style.display = 'block';
    const tbody = document.querySelector('#equitiesTable tbody');
    tbody.innerHTML = pageData.content.map(eq => `
        <tr>
            <td><input type="radio" name="selectedEquity" value="${eq.equityId}"></td>
            <td>${eq.tradingSymbol}</td>
            <td>${eq.segment.name}</td>
            <td>${eq.exchange.name}</td>
        </tr>
    `).join('');

    document.getElementById('paginationControls').innerHTML = `
        Page: ${currentPage + 1} / ${pageData.totalPages}
        <button ${currentPage === 0 ? 'disabled' : ''} onclick="fetchEquities('${currentSegment}', '${currentExchange}', '${currentSearch}', ${currentPage - 1}, ${pageSize})">Prev</button>
        <button ${currentPage === pageData.totalPages - 1 ? 'disabled' : ''} onclick="fetchEquities('${currentSegment}', '${currentExchange}', '${currentSearch}', ${currentPage + 1}, ${pageSize})">Next</button>
    `;
}

function loadEquities(event) {
    event.preventDefault();
    const segment = document.getElementById('segmentSelect').value;
    const exchange = document.getElementById('exchangeSelect').value;
    console.log("Fetching with segment:", segment, "exchange:", exchange);
    fetchEquities(segment, exchange, '', 0, pageSize);
}

function searchEquities() {
    const search = document.getElementById('searchEquity').value;
    fetchEquities(currentSegment, currentExchange, search, 0, pageSize);
}

function increasePageSize() {
    fetchEquities(currentSegment, currentExchange, currentSearch, 0, pageSize + 25);
}

function submitEquity() {
    const selectedId = document.querySelector('input[name="selectedEquity"]:checked');
    if (!selectedId) {
        alert('Please select an equity first.');
        return;
    }

    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const interval = document.getElementById('intervalSelect').value;

    const params = new URLSearchParams({
        equityId: selectedId.value,
        fromDate: startDate,
        toDate: endDate,
        interval: interval
    });

    fetch(`/api/backtest/historical-data?${params.toString()}`)
        .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
        .then(data => {
        console.log('Historical data received:', data);
        // Render candlestick chart with received data
        renderCandlestickChart(data);
    })
        .catch(error => {
        console.error('Error fetching historical data:', error);
        alert('Failed to fetch historical data.');
    });
}

function loadBackTestPanel() {
    console.log("loadBackTestPanel() called");
    fetch('/historical/init')
        .then(res => res.json())
        .then(data => {
        const segmentSelect = document.getElementById('segmentSelect');
        const exchangeSelect = document.getElementById('exchangeSelect');
        // Populate segment dropdown
        segmentSelect.innerHTML = data.segments.map(s =>
        `<option value="${s}">${s}</option>`).join('');

        // Populate exchange dropdown
        exchangeSelect.innerHTML = data.exchanges.map(e =>
        `<option value="${e}">${e}</option>`).join('');
        // Attach form submit handler
        const submitBtn = document.getElementById('segmentExchangeSubmit');
        submitBtn.addEventListener('click', loadEquities);
    });
}

function renderCandlestickChart(data) {
    // Clear previous chart if any
    document.getElementById('candlestickChart').innerHTML = '';

    // Create the chart
    const chart = LightweightCharts.createChart(document.getElementById('candlestickChart'), {
        width: document.getElementById('candlestickChart').offsetWidth,
        height: 450,
        layout: {
            background: { color: '#fff' },
            textColor: '#222',
        },
        grid: {
            vertLines: { color: '#eee' },
            horzLines: { color: '#eee' },
        },
        crosshair: {
            mode: LightweightCharts.CrosshairMode.Normal,
        },
        timeScale: {
            timeVisible: true,
            secondsVisible: false,
            borderColor: '#ccc'
        },
        rightPriceScale: {
            borderColor: '#ccc'
        }
    });

    // Prepare candlestick data
    const candleSeries = chart.addCandlestickSeries({
        upColor: '#00e396',
        downColor: '#ff4560',
        borderVisible: false,
        wickUpColor: '#00e396',
        wickDownColor: '#ff4560'
    });

    // Map your data to the required format
    const candleData = data.map(point => ({
        time: point.timeStamp.length > 10
        ? point.timeStamp.slice(0, 10) // 'YYYY-MM-DD'
        : point.timeStamp,
        open: point.open,
        high: point.high,
        low: point.low,
        close: point.close,
    }));

    candleSeries.setData(candleData);

    // Info bar logic
    const ohlcBar = document.getElementById('ohlc-info-bar');
    function showOhlcInfo(bar) {
        if (!bar) {
            ohlcBar.style.display = 'none';
            return;
        }
        ohlcBar.style.display = 'flex';
        ohlcBar.innerHTML = `
            <span class="ohlc-label">O</span> ${bar.open}
            <span class="ohlc-label">H</span> ${bar.high}
            <span class="ohlc-label">L</span> ${bar.low}
            <span class="ohlc-label">C</span> ${bar.close}
            <span class="ohlc-label">Date</span> ${bar.time}
        `;
    }

    // Show last candle on load
    showOhlcInfo(candleData[candleData.length - 1]);

    // Update info bar on crosshair move
    chart.subscribeCrosshairMove(param => {
        // Check if mouse is over a valid bar
        const bar = param && param.seriesData && param.seriesData.get(candleSeries);
        if (bar && param.time) {
            showOhlcInfo({
                open: bar.open,
                high: bar.high,
                low: bar.low,
                close: bar.close,
                time: typeof param.time === 'string'
                ? param.time
                : new Date(param.time * 1000).toISOString().slice(0, 10)
            });
        } else {
            // Show last candle if not over a bar
            showOhlcInfo(candleData[candleData.length - 1]);
        }
    });

    // Responsive resize
    window.addEventListener('resize', () => {
        chart.applyOptions({ width: document.getElementById('candlestickChart').offsetWidth });
    });

    // Show chart container
    document.getElementById('candlestickChartContainer').style.display = 'block';
}