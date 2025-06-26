let currentPage = 0;
let pageSize = 25;
let currentSegment = '';
let currentExchange = '';
let currentSearch = '';
var validData = [];
var volumeSeries;
let mainChart, mainCandleSeries;
let crosshairHandler;
let currentChartData = [];

function showOhlcInfo(bar, interval) {
    const ohlcBar = document.getElementById('ohlc-info-bar');
    if (!ohlcBar) return;

    if (!bar) {
        ohlcBar.style.display = 'none';
        return;
    }

    ohlcBar.style.display = 'flex';

    let dateStr = '';

    if (interval === 'day' && typeof bar.time === 'object') {
        // BusinessDay format (Lightweight Charts expects this)
        dateStr = `${String(bar.time.day).padStart(2, '0')}-${String(bar.time.month).padStart(2, '0')}-${bar.time.year}`;
    } else {
        // UNIX timestamp format
        const date = new Date(bar.time * 1000);
        dateStr = date.toLocaleString('en-IN', {
            timeZone: 'Asia/Kolkata',
            day: '2-digit', month: 'short', year: 'numeric',
            hour: interval === 'day' ? undefined : '2-digit',
            minute: interval === 'day' ? undefined : '2-digit'
        });
    }

    ohlcBar.innerHTML = `
        <span class="ohlc-label">O</span> ${bar.open}
        <span class="ohlc-label">H</span> ${bar.high}
        <span class="ohlc-label">L</span> ${bar.low}
        <span class="ohlc-label">C</span> ${bar.close}
        <span class="ohlc-label">Vol</span> ${bar.volume || '-'}
        <span class="ohlc-label">Date</span> ${dateStr}
    `;
}

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
        currentChartData = data;
        renderCandlestickChart(data, interval);
        plotSupportResistanceMarkers(data);
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
function getTimeValue(d, interval) {
    const date = new Date(d.timeStamp);
    if (interval === 'day') {
        return {
            year: date.getFullYear(),
            month: date.getMonth() + 1,
            day: date.getDate()
        };
    } else {
        return Math.floor(date.getTime() / 1000);
    }
}


function renderCandlestickChart(data, interval) {
    const uniqueData = [];
    clearChartOnNewInterval();
    mainChart = LightweightCharts.createChart(document.getElementById('candlestickChart'), {
        autoSize: true,
        layout: { background: { color: '#fff' }, textColor: '#000' },
        grid: { vertLines: { color: '#eee' }, horzLines: { color: '#eee' } },
        crosshair: { mode: LightweightCharts.CrosshairMode.Normal },
        rightPriceScale: {
            borderColor: '#ccc',
            visible: true,    // Main candle price scale
            scaleMargins: { top: 0.05, bottom: 0.3 } // leave space for volume below
        },
        timeScale: {
            borderColor: '#ccc',
            timeVisible: interval !== 'day', // timeVisible false = only date shown
            secondsVisible: false
        },
        localization: {
            timeFormatter: (time) => {
                if (typeof time === 'object' && time.year) {
                    // BusinessDay format
                    return `${String(time.day).padStart(2, '0')}-${String(time.month).padStart(2, '0')}-${time.year}`;
                } else {
                    // UNIX timestamp (seconds)
                    const date = new Date(time * 1000);
                    return date.toLocaleString('en-IN', {
                        timeZone: 'Asia/Kolkata',
                        day: '2-digit', month: 'short', year: 'numeric',
                        hour: interval === 'day' ? undefined : '2-digit',
                        minute: interval === 'day' ? undefined : '2-digit'
                    });
                }
            }
        }
    });

    // Clean and deduplicate data
    const cleanedData = cleanCandlestickData(data, interval);
    let lastTime = null;
    for (const d of cleanedData) {
        if (d.time !== lastTime) {
            uniqueData.push(d);
            lastTime = d.time;
        }
    }
    mainCandleSeries = mainChart.addCandlestickSeries();

    // Set data
    validData = uniqueData;
    mainCandleSeries.setData(uniqueData);

    // Logging (fix .size to .length)
    console.log('Setting candlestick data size:', uniqueData.length);
    console.log('Setting candlestick data:', uniqueData);
    console.log('First 5 data points:', uniqueData.slice(0, 5));
    console.log('Last 5 data points:', uniqueData.slice(-5));
    console.log('Data is sorted:', uniqueData.every((d, i, arr) => i === 0 || d.time >= arr[i-1].time));
    console.log('Duplicates:', uniqueData.some((d, i, arr) => i > 0 && d.time === arr[i-1].time));
    console.log('Chart container size:', document.getElementById('candlestickChart').offsetWidth, document.getElementById('candlestickChart').offsetHeight);

    // Show last candle info by default
    showOhlcInfo(validData[validData.length - 1], interval);

    // Subscribe to crosshair move only once, after chart and series are created
    crosshairHandler = (param) => {
        if (!param || !param.time) {
            showOhlcInfo(validData[validData.length - 1], interval);
            return;
        }
        const bar = param.seriesData.get(mainCandleSeries);
        if (bar) {
            const dataBar = validData.find(d => d.time === param.time);
            showOhlcInfo({
                open: bar.open,
                high: bar.high,
                low: bar.low,
                close: bar.close,
                volume: dataBar ? dataBar.volume : '',
                time: param.time
            }, interval);
        } else {
            showOhlcInfo(validData[validData.length - 1], interval);
        }
    };
    mainChart.subscribeCrosshairMove(crosshairHandler);

    document.getElementById('candlestickChartContainer').style.display = 'flex';
    document.getElementById('candlestickChartContainer').style.flexDirection = 'column';
}

function clearChartOnNewInterval() {
    // Remove all indicator series
    clearAllIndicators();
    if (mainChart) {
        mainChart.remove();
        mainChart = null; // Allow clean re-creation
    }
}

function cleanCandlestickData(data, interval) {
    return data
        .filter(d =>
    d.open !== null && d.open !== undefined &&
    d.high !== null && d.high !== undefined &&
    d.low !== null && d.low !== undefined &&
    d.close !== null && d.close !== undefined &&
    d.volume !== null && d.volume !== undefined &&
    d.timeStamp !== null && d.timeStamp !== undefined
    )
        .map(d => ({
        open: Number(d.open),
        high: Number(d.high),
        low: Number(d.low),
        close: Number(d.close),
        volume: Number(d.volume),
        time: getTimeValue(d, interval)
    })).sort((a, b) => {
        if (typeof a.time === 'string' && typeof b.time === 'string') {
            return a.time.localeCompare(b.time);
        }
        return a.time - b.time;
    });
}

// Sample: Add support/resistance markers
function plotSupportResistanceMarkers(dataWithLevels) {
    const markers = [];

    dataWithLevels.forEach(point => {
        const time = new Date(point.timeStamp).getTime() / 1000; // convert ms to seconds

        if (point.support) {
            markers.push({
                time: time,
                position: 'belowBar',
                color: '#26a69a', // Green for support
                shape: 'arrowUp',
                text: 'Support'
            });
        }

        if (point.resistance) {
            markers.push({
                time: time,
                position: 'aboveBar',
                color: '#ef5350', // Red for resistance
                shape: 'arrowDown',
                text: 'Resistance'
            });
        }
    });

    mainCandleSeries.setMarkers(markers);
}