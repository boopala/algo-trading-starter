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

// You need to include dayjs for date formatting
// <script src="https://cdn.jsdelivr.net/npm/dayjs@1/dayjs.min.js"></script>

let lastHoveredIndex = null;

function renderCandlestickChart(data) {
    const chartContainer = document.querySelector('#backTestPanel #candlestickChart');
    if (!chartContainer) {
        console.error("Chart container not found in backtest panel.");
        return;
    }
    const categories = data.map(point => point.timeStamp);

    const seriesData = data.map((point, idx) => ({
        x: idx,
        y: [point.open, point.high, point.low, point.close]
    }));

    const chartOptions = {
        chart: {
            type: 'candlestick',
            height: 400,
            id: 'candlestickChart',
            animations: {
                enabled: true,
                easing: 'easeinout',
                speed: 800,
            },
            toolbar: {
                show: true,
                tools: {
                    download: true,
                    selection: true,
                    zoom: true,
                    zoomin: true,
                    zoomout: true,
                    pan: true,
                    reset: true,
                },
                autoSelected: 'zoom'
            },
            events: {
                mouseMove: function(event, chartContext, config) {
                    // Show info bar on hover
                    if (config.dataPointIndex !== -1 && config.dataPointIndex !== lastHoveredIndex) {
                        lastHoveredIndex = config.dataPointIndex;
                        showOhlcInfo(data[config.dataPointIndex]);
                    }
                },
                mouseLeave: function() {
                    lastHoveredIndex = null;
                    clearOhlcInfo();
                },
                zoomed: function(chartContext, {xaxis}) {
                    updateXAxisFormat(chartContext, xaxis);
                }
            }
        },
        plotOptions: {
            candlestick: {
                colors: {
                    upward: '#00e396',
                    downward: '#ff4560'
                },
                wick: {
                    useFillColor: true,
                }
            }
        },
        grid: {
            show: true,
            borderColor: '#e7e7e7',
            strokeDashArray: 4,
            position: 'back',
            xaxis: { lines: { show: true } },
            yaxis: { lines: { show: true } },
        },
        tooltip: {
            enabled: false // We'll use our own info bar
        },
        series: [{
            name: "Price",
            data: seriesData
        }],
        xaxis: {
            type: 'category',
            categories: categories,
            labels: {
                rotate: -45,
                formatter: function(val, idx) {
                    if (!categories[idx]) return '';
                    // Default to date, will update on zoom
                    return dayjs(categories[idx]).format('DD MMM YYYY');
                },
                style: { colors: '#333', fontSize: '12px' }
            },
            tooltip: {
                enabled: false
            }
        },
        yaxis: {
            tooltip: { enabled: true },
            labels: { style: { colors: '#333', fontSize: '12px' } }
        },
        title: {
            text: 'Candlestick Chart',
            align: 'left',
            style: { fontSize: '20px', color: '#263238' }
        }
    };

    // Destroy old chart if exists
    if (window.candleChart) {
        window.candleChart.destroy();
    }
    window.candleChart = new ApexCharts(document.querySelector("#backTestPanel #candlestickChart"), chartOptions);
    window.candleChart.render();

    // Show the latest candle's info on initial load
    showOhlcInfo(data[data.length - 1]);
}

// Info bar logic
function showOhlcInfo(point) {
    const ohlcBar = document.getElementById('ohlc-info-bar');
    if (!point) return;
    ohlcBar.innerHTML = `
        <span class="ohlc-label">O</span> ${point.open}
        <span class="ohlc-label">H</span> ${point.high}
        <span class="ohlc-label">L</span> ${point.low}
        <span class="ohlc-label">C</span> ${point.close}
        <span class="ohlc-label">Date</span> ${dayjs(point.timeStamp).format('DD MMM YYYY')}
    `;
    ohlcBar.style.display = 'flex';
}

function clearOhlcInfo() {
    const ohlcBar = document.getElementById('ohlc-info-bar');
    ohlcBar.innerHTML = '';
    ohlcBar.style.display = 'none';
}

function updateXAxisFormat(chartContext, xaxis) {
    // xaxis.min and xaxis.max are indices for category axis
    // Get your categories array (timestamps as ISO strings)
    const categories = chartContext.w.globals.labels;
    if (!categories || categories.length === 0) return;

    // Convert indices to actual time values
    const minIdx = Math.max(Math.floor(xaxis.min), 0);
    const maxIdx = Math.min(Math.ceil(xaxis.max), categories.length - 1);

    const minDate = new Date(categories[minIdx]);
    const maxDate = new Date(categories[maxIdx]);
    const MS_PER_DAY = 24 * 60 * 60 * 1000;
    const daysVisible = Math.max(1, (maxDate - minDate) / MS_PER_DAY);

    // Decide label format
    let formatter;
    if (daysVisible <= 7) {
        // Show date and time if zoomed in to a week or less
        formatter = function(val, idx) {
            if (!categories[idx]) return '';
            const d = new Date(categories[idx]);
            return d.toLocaleString(undefined, { day: '2-digit', month: 'short', year: '2-digit', hour: '2-digit', minute: '2-digit' });
        };
    } else {
        // Show only date if zoomed out
        formatter = function(val, idx) {
            if (!categories[idx]) return '';
            const d = new Date(categories[idx]);
            return d.toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: '2-digit' });
        };
    }

    // Update chart x-axis labels
    chartContext.updateOptions({
        xaxis: {
            labels: {
                formatter: formatter
            }
        }
    });
}

