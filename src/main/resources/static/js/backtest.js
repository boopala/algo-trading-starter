let currentPage = 0;
let pageSize = 25;
let currentSegment = '';
let currentExchange = '';
let currentSearch = '';
var validData = [];
var candleSeries;
var volumeSeries;
let crosshairSubscribed = false;

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
        const invalidData = data.filter(d =>
        d.open === null || d.high === null || d.low === null ||
        d.close === null || d.timeStamp === null
        );
        console.log("Invalid data points:", invalidData);
        // Render candlestick mainChart with received data
        renderCandlestickChart(data, interval);
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

// Helper: get time value in correct format for Lightweight Charts
function getTimeValue(point, interval) {
    if (interval === 'day') {
        return point.timeStamp.slice(0, 10); // "YYYY-MM-DD"
    } else {
        return Math.floor(new Date(fixTimeZone(point.timeStamp)).getTime() / 1000); // UNIX seconds
    }
}

function fixTimeZone(ts) {
    // Converts "2022-01-03T09:15:00+0530" to "2022-01-03T09:15:00+05:30"
    return ts.replace(/([+-]\d{2})(\d{2})$/, "$1:$2");
}

function renderCandlestickChart(data, interval) {
    const uniqueData = [];
    clearChartOnNewInterval();

    if (!mainChart) {
        createMainChart(data, interval);
    } else {
        // Clean and prepare data
        validData = data
            .filter(d =>
        d.open !== null && d.open !== undefined &&
        d.high !== null && d.high !== undefined &&
        d.low !== null && d.low !== undefined &&
        d.close !== null && d.close !== undefined &&
        d.volume !== null && d.volume !== undefined &&
        d.timeStamp
        )
            .map(d => ({
            time: getTimeValue(d, interval),
            open: Number(d.open),
            high: Number(d.high),
            low: Number(d.low),
            close: Number(d.close),
            volume: Number(d.volume)
        }))
            .sort((a, b) => {
            if (typeof a.time === 'string' && typeof b.time === 'string') {
                return a.time.localeCompare(b.time);
            }
            return a.time - b.time;
        });
        // Remove duplicates
        let lastTime = null;
        for (const d of validData) {
            if (d.time !== lastTime) {
                uniqueData.push(d);
                lastTime = d.time;
            }
        }
        if (!candleSeries) {
            candleSeries = mainChart.addCandlestickSeries({
                priceScaleId: 'right'
            });
        }
        validData = uniqueData;
        candleSeries.setData(uniqueData);
    }

    const ohlcBar = document.getElementById('ohlc-info-bar');
    function showOhlcInfo(bar) {
        if (!bar) {
            ohlcBar.style.display = 'none';
            return;
        }
        ohlcBar.style.display = 'flex';
        const dateStr = (interval === 'day')
        ? bar.time // 'YYYY-MM-DD'
        : new Date(bar.time * 1000).toLocaleString('en-IN', {
            day: '2-digit', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });

        ohlcBar.innerHTML = `
            <span class="ohlc-label">O</span> ${bar.open}
            <span class="ohlc-label">H</span> ${bar.high}
            <span class="ohlc-label">L</span> ${bar.low}
            <span class="ohlc-label">C</span> ${bar.close}
            <span class="ohlc-label">Vol</span> ${bar.volume}
            <span class="ohlc-label">Date</span> ${dateStr}
        `;
    }

    // Show last candle's info by default
    showOhlcInfo(validData[validData.length - 1]);

    // Subscribe to crosshair move only once
    /*if (!crosshairSubscribed) {
        mainChart.subscribeCrosshairMove(param => {
            if (!param || !param.time) {
                showOhlcInfo(validData[validData.length - 1]);
                return;
            }
            const bar = param.seriesData.get(candleSeries);
            if (bar) {
                const dataBar = validData.find(d => d.time === param.time);
                showOhlcInfo({
                    open: bar.open,
                    high: bar.high,
                    low: bar.low,
                    close: bar.close,
                    volume: dataBar ? dataBar.volume : '',
                    time: param.time
                });
            } else {
                showOhlcInfo(validData[validData.length - 1]);
            }
        });
        crosshairSubscribed = true;
    }*/

    document.getElementById('candlestickChartContainer').style.display = 'flex';
    document.getElementById('candlestickChartContainer').style.flexDirection = 'column';
}

function clearChartOnNewInterval() {
    // Remove all indicator series
    if (typeof indicatorSeriesMap !== "undefined") {
        Object.keys(indicatorSeriesMap).forEach(key => {
            if (indicatorSeriesMap[key] && indicatorSeriesMap[key].series) {
                console.log('Removing series for key:', key);
                mainChart.removeSeries(indicatorSeriesMap[key].series);
            } else {
                console.warn('No series to remove for key:', key, indicatorSeriesMap[key]);
            }
            delete indicatorSeriesMap[key];
        });
    }

    // Remove volume series if present
    if (typeof volumeSeries !== "undefined" && volumeSeries) {
        mainChart.removeSeries(volumeSeries);
        volumeSeries = null;
        delete indicatorSeriesMap['Volume'];
    }
    renderIndicatorList();
}