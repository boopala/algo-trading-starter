window.indicatorSeriesMap = window.indicatorSeriesMap || {};
let rsiToMainHandler = null;
let mainToRsiHandler = null;
var rsiChart = null;

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
                lineWidth: 1
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
        indicatorSeriesMap[key] = { series: volumeSeries, chart: mainChart, meta: { color: '#888', width: 1 } };
        renderIndicatorList();
        return;
    }

    if (!indicator || !['SMA', 'EMA', 'Volume', 'RSI'].includes(indicator)) {
        alert("Select SMA, EMA, Volume, or RSI.");
        return;
    }

    const response = await invokeCustomIndicator(indicator, period, validData);
    const result = await response.json();
    const key = `${indicator}_${period}`;

    if (result.values && result.values.length) {
        if (indicatorSeriesMap[key]) {
            const info = indicatorSeriesMap[key];
            if (info.series) {
                if (Array.isArray(info.series)) {
                    info.series.forEach(s => mainChart.removeSeries(s));
                } else {
                    mainChart.removeSeries(info.series);
                }
            }
            delete indicatorSeriesMap[key];
        }

        const data = result.values.map((val, idx) => ({
            time: validData[idx].time,
            value: val
        }));
        if (indicator === 'RSI') {
            const mainChartContainer = document.getElementById('candlestickChart');
            const rsiChartContainer = document.getElementById('rsiChart');
            if (!rsiChartContainer) {
                alert('RSI container not found!');
                return;
            }
            const chartWidth = mainChartContainer.clientWidth;
            if (rsiChart) {
                rsiChart.remove(); // remove old RSI chart if exists
                rsiChart = null;
            }
            rsiChart = LightweightCharts.createChart(rsiChartContainer, {
                width: chartWidth, height: 100,
                layout: { background: { color: '#fff' }, textColor: '#000' },
                grid: { vertLines: { color: '#eee' }, horzLines: { color: '#eee' } },
                crosshair: { mode: LightweightCharts.CrosshairMode.Normal },
                rightPriceScale: {
                    visible: true,
                    scaleMargins: { top: 0.15, bottom: 0.15 }
                },
                timeScale: {
                    visible: true,
                    timeVisible: true,
                    secondsVisible: false
                }
            });
            subscribeRSISync();
            const rsiSeries = rsiChart.addLineSeries({
                color,
                lineWidth: width
            });
            rsiSeries.setData(data);
            indicatorSeriesMap[key] = { series: rsiSeries, chart: rsiChart, meta: { color, width } };
        } else {
            // Add new line series
            const series = mainChart.addLineSeries({ color, lineWidth: width });
            series.setData(data);
            // Store series and its metadata
            indicatorSeriesMap[key] = { series, chart: mainChart, meta: { color, width } };
        }
        // Render the updated indicator list
        renderIndicatorList();
    }
}

// Remove a single indicator
function removeSpecificIndicator(key) {
    const info = indicatorSeriesMap[key];

    if (!info || !info.series) {
        console.warn(`Indicator ${key} not found or already removed.`);
        return;
    }

    const chart = info.chart || mainChart;

    if (Array.isArray(info.series)) {
        info.series.forEach(series => {
            if (series) chart.removeSeries(series);
        });
    } else {
        if (info.series) chart.removeSeries(info.series);
    }

    // Cleanup
    delete indicatorSeriesMap[key];
    renderIndicatorList();

    // If it's RSI, clear container
    if (key.startsWith('RSI')) {
        const rsiChartContainer = document.getElementById('rsiChart');
        if (rsiChartContainer) rsiChartContainer.innerHTML = '';
        if (typeof rsiChart !== 'undefined') {
            rsiChart = null;
        }
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
        const info = indicatorSeriesMap[key];
        if (info.series) {
            if (Array.isArray(info.series)) {
                info.series.forEach(s => {
                    const chart = info.chart || mainChart;
                    chart.removeSeries(s);
                });
            } else {
                const chart = info.chart || mainChart;
                chart.removeSeries(info.series);
            }
        }
        delete indicatorSeriesMap[key];
    });

    // RSI Chart cleanup (if RSI is created separately)
    if (rsiChart) {
        unsubscribeRSISync && unsubscribeRSISync(); // optional if sync exists
        rsiChart.remove();
        rsiChart = null;
    }
    renderIndicatorList();
}


function initializeChartControls() {
    const NAV_BARS = 20;
    const chartLeftBtn = document.getElementById('chartLeftBtn');
    const chartRightBtn = document.getElementById('chartRightBtn');
    const chartFullscreenBtn = document.getElementById('chartFullscreenBtn');
    const chartContainer = document.getElementById('candlestickChartContainer');

    chartLeftBtn.onclick = function() {
        if (mainChart) {
            mainChart.timeScale().scrollToPosition(
                mainChart.timeScale().scrollPosition() - NAV_BARS,
                false
            );
        }
    };

    chartRightBtn.onclick = function() {
        if (mainChart) {
            mainChart.timeScale().scrollToPosition(
                mainChart.timeScale().scrollPosition() + NAV_BARS,
                false
            );
        }
    };

    chartFullscreenBtn.onclick = function() {
        if (!document.fullscreenElement) {
            chartContainer.requestFullscreen().catch(err => console.error("Fullscreen error:", err));
        } else {
            document.exitFullscreen();
        }
    };

    // Centralized chart resizing function
    function resizeCharts() {
        const chartDiv = document.getElementById('candlestickChart');
        const rsiDiv = document.getElementById('rsiChart');
        const isFullScreen = document.fullscreenElement === chartContainer;
        const width = chartContainer.clientWidth;

        if (isFullScreen) {
            const screenHeight = window.innerHeight;
            const width = chartContainer.clientWidth;

            const rsiDiv = document.getElementById('rsiChart');
            const hasRSI = rsiDiv && rsiDiv.childElementCount > 0;

            if (hasRSI) {
                const mainHeight = Math.floor(screenHeight * 0.75);
                const rsiHeight = screenHeight - mainHeight;

                chartDiv.style.height = `${mainHeight}px`;
                rsiDiv.style.height = `${rsiHeight}px`;

                if (mainChart) mainChart.resize(width, mainHeight);
                if (rsiChart) rsiChart.resize(width, rsiHeight);
            } else {
                chartDiv.style.height = `${screenHeight}px`;
                if (mainChart) mainChart.resize(width, screenHeight);
                if (rsiChart) rsiChart.resize(width, 0); // collapse RSI
            }
        } else {
            chartDiv.style.width = '100%';
            chartDiv.style.height = '400px';
            if (mainChart) mainChart.resize(width, 400);

            if (rsiDiv) {
                rsiDiv.style.width = '100%';
                rsiDiv.style.height = '100px';
                rsiDiv.style.minHeight = '100px';
                rsiDiv.style.display = 'block';
                if (rsiChart) rsiChart.resize(width, 100);
            }
        }
    }

    let resizeTimeout;
    function debounceResizeCharts() {
        clearTimeout(resizeTimeout);
        resizeTimeout = setTimeout(resizeCharts, 100);
    }

    document.addEventListener('fullscreenchange', debounceResizeCharts);
    window.addEventListener('resize', debounceResizeCharts);
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

function subscribeRSISync() {
    rsiToMainHandler = () => {
        if (mainChart && rsiChart) {
            const mainRange = rsiChart.timeScale().getVisibleRange();
            if (mainRange) mainChart.timeScale().setVisibleRange(mainRange);
        }
    };
    mainToRsiHandler = () => {
        if (mainChart && rsiChart) {
            const rsiRange = mainChart.timeScale().getVisibleRange();
            if (rsiRange) rsiChart.timeScale().setVisibleRange(rsiRange);
        }
    };

    rsiChart.timeScale().subscribeVisibleTimeRangeChange(rsiToMainHandler);
    mainChart.timeScale().subscribeVisibleTimeRangeChange(mainToRsiHandler);
}

function unsubscribeRSISync() {
    if (rsiChart && rsiToMainHandler) {
        rsiChart.timeScale().unsubscribeVisibleTimeRangeChange(rsiToMainHandler);
    }
    if (mainChart && mainToRsiHandler) {
        mainChart.timeScale().unsubscribeVisibleTimeRangeChange(mainToRsiHandler);
    }
}

window.addEventListener('DOMContentLoaded', () => {
    const toggleSR = document.getElementById('toggleSR');
    if (toggleSR) {
        toggleSR.addEventListener('change', (e) => {
            if (e.target.checked) {
                plotSupportResistanceMarkers(currentChartData); // your function
            } else {
                mainCandleSeries.setMarkers([]); // Clear markers
            }
        });
    } else {
        console.warn("toggleSR checkbox not found in the DOM.");
    }
});

