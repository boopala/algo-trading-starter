let mainChart, mainCandleSeries;
window.indicatorSeriesMap = window.indicatorSeriesMap || {};

function createMainChart(data, interval) {
    mainChart = LightweightCharts.createChart(document.getElementById('candlestickChart'), {
        autoSize: true,
        layout: { background: { color: '#fff' }, textColor: '#000' },
        grid: { vertLines: { color: '#eee' }, horzLines: { color: '#eee' } },
        crosshair: { mode: LightweightCharts.CrosshairMode.Normal },
        rightPriceScale: { borderColor: '#ccc' },
        timeScale: {
            borderColor: '#ccc',
            timeVisible: true,      // <-- This is essential!
        }
    });

    // Clean and deduplicate data
    const cleanedData = cleanCandlestickData(data, interval);
    const uniqueData = [];
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

    // OHLC info bar logic
    const ohlcBar = document.getElementById('ohlc-info-bar');
    function showOhlcInfo(bar) {
        if (!bar) {
            ohlcBar.style.display = 'none';
            return;
        }
        ohlcBar.style.display = 'flex';
        const dateStr = (interval === 'day')
        ? bar.time
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

    // Show last candle info by default
    showOhlcInfo(validData[validData.length - 1]);

    // Subscribe to crosshair move only once, after chart and series are created
    mainChart.subscribeCrosshairMove(param => {
        if (!param || !param.time) {
            showOhlcInfo(validData[validData.length - 1]);
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
            });
        } else {
            showOhlcInfo(validData[validData.length - 1]);
        }
    });
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

function getIndicatorListElement() {
    return document.getElementById('indicatorList');
}

function renderIndicatorList() {
    const indicatorList = getIndicatorListElement();
    indicatorList.innerHTML = ''; // Clear previous

    Object.keys(indicatorSeriesMap).forEach(key => {
        const span = document.createElement('span');
        span.className = 'indicator-badge';
        span.dataset.key = key;
        span.innerHTML = `${key} <span class="close-icon">X</span>`;
        span.querySelector('.close-icon').onclick = function(e) {
            e.stopPropagation();
            removeSpecificIndicator(key);
        };
        indicatorList.appendChild(span);
    });
}

// Apply (add/update) indicator
async function applyCustomIndicator() {
    const indicator = document.getElementById('indicatorSelect').value;
    const period = parseInt(document.getElementById('periodInput').value, 10);
    const color = document.getElementById('colorInput').value;
    const width = parseInt(document.getElementById('widthInput').value, 10);

    // Handle Volume as a special case
    if (indicator === 'Volume') {
        const key = 'Volume';
        if (!volumeSeries) {
            volumeSeries = mainChart.addHistogramSeries({
                priceScaleId: '',
                priceFormat: { type: 'volume' },
                overlay: false,
                scaleMargins: { top: 0.85, bottom: 0 },
                lineWidth: 1,
            });
            // Prepare volume data with color
            const volumeData = validData.map(bar => {
                let barColor;
                if (bar.close > bar.open) barColor = 'rgba(38, 166, 154, 0.4)';
                else if (bar.close < bar.open) barColor = 'rgba(239, 83, 80, 0.4)';
                else barColor = 'rgba(153, 153, 153, 0.4)';
                return { time: bar.time, value: bar.volume, color: barColor };
            });
            volumeSeries.setData(volumeData);
        }
        // Add to indicator map for consistent handling
        indicatorSeriesMap[key] = { series: volumeSeries, meta: { color: '#888', width: 1 } };
        renderIndicatorList();
        return;
    }

    if (!indicator || !(indicator === 'SMA' || indicator === 'EMA' || indicator === 'Volume')) {
        alert("Select SMA, EMA, or Volume.");
        return;
    }

    const response = await invokeCustomIndicator(indicator, period, validData);
    const result = await response.json();
    const key = `${indicator}_${period}`;

    if (result.values && result.values.length) {
        // Remove existing indicator if present
        if (indicatorSeriesMap[key]) {
            mainChart.removeSeries(indicatorSeriesMap[key].series);
            delete indicatorSeriesMap[key];
        }
        // Prepare data for the indicator line
        const data = result.values.map((val, idx) => ({
            time: validData[idx].time,
            value: val
        }));
        // Add new line series
        const series = mainChart.addLineSeries({ color, lineWidth: width });
        series.setData(data);
        // Store series and its metadata
        indicatorSeriesMap[key] = { series, meta: { color, width } };
        // Render the updated indicator list
        renderIndicatorList();
    }
}

// Remove a single indicator
function removeSpecificIndicator(key) {
    if (key === 'Volume' && volumeSeries) {
        mainChart.removeSeries(volumeSeries);
        volumeSeries = null;
        delete indicatorSeriesMap[key];
        renderIndicatorList();
        return;
    }
    if (indicatorSeriesMap[key]) {
        mainChart.removeSeries(indicatorSeriesMap[key].series);
        delete indicatorSeriesMap[key];
        renderIndicatorList();
    }
}

// Clear all indicators
function clearAllIndicators() {
    if (volumeSeries) {
        mainChart.removeSeries(volumeSeries);
        volumeSeries = null;
        delete indicatorSeriesMap['Volume'];
    }
    Object.keys(indicatorSeriesMap).forEach(key => {
        mainChart.removeSeries(indicatorSeriesMap[key].series);
        delete indicatorSeriesMap[key];
    });
    renderIndicatorList();
}

function initializeChartControls() {
    const NAV_BARS = 20;
    const chartLeftBtn = document.getElementById('chartLeftBtn');
    const chartRightBtn = document.getElementById('chartRightBtn');
    const chartFullscreenBtn = document.getElementById('chartFullscreenBtn');
    const chartContainer = document.getElementById('candlestickChartContainer');
    let isFullscreen = false;

    chartLeftBtn.onclick = function() {
        mainChart.timeScale().scrollToPosition(mainChart.timeScale().scrollPosition() - NAV_BARS, false);
    };
    chartRightBtn.onclick = function() {
        mainChart.timeScale().scrollToPosition(mainChart.timeScale().scrollPosition() + NAV_BARS, false);
    };
    chartFullscreenBtn.onclick = function() {
        if (!document.fullscreenElement) {
            chartContainer.requestFullscreen();
        } else {
            document.exitFullscreen();
        }
    };
    document.addEventListener('fullscreenchange', () => {
        const chartDiv = document.getElementById('candlestickChart');
        if (document.fullscreenElement === chartContainer) {
            // Resize chart to fill fullscreen
            chartDiv.style.width = '100vw';
            chartDiv.style.height = '100vh';
            // Call your chart library's resize method if needed
            // mainChart.resize(window.innerWidth, window.innerHeight);
        } else {
            // Restore original size
            chartDiv.style.width = '100%';
            chartDiv.style.height = '400px';
            // mainChart.resize(originalWidth, originalHeight);
        }
    });

    document.addEventListener('fullscreenchange', () => {
        if (document.fullscreenElement === chartContainer) {
            mainChart.resize(window.innerWidth, window.innerHeight);
        } else {
            mainChart.resize(900, 400);
        }
    });
}

window.onload = function() {
    initializeChartControls();

    document.getElementById('addIndicatorBtn').onclick = applyCustomIndicator;
    document.getElementById('clearIndicatorsBtn').onclick = clearAllIndicators;
};

async function invokeCustomIndicator(indicator, period, validData) {
    const open = validData.map(d => d.open);
    const high = validData.map(d => d.high);
    const low = validData.map(d => d.low);
    const close = validData.map(d => d.close);
    const volume = validData.map(d => d.volume);
    const response = await fetch('/api/indicators/customIndicator', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ indicator, period, open, high, low, close, volume })
    });
    return response;
}

