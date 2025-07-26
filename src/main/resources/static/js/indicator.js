window.indicatorSeriesMap = window.indicatorSeriesMap || {};
let rsiToMainHandler = null;
let mainToRsiHandler = null;
var rsiChart = null;

document.addEventListener('DOMContentLoaded', function () {
    const macdOverlay = document.getElementById('macdModalOverlay');
    if (macdOverlay) {
        macdOverlay.addEventListener('click', function (event) {
            if (event.target === this) {
                closeMacdModal();
            }
        });
    }
});


function openMacdModal() {
    const overlay = document.getElementById('macdModalOverlay');
    if (overlay) {
        overlay.style.display = 'flex'; // Centered with transparent bg
        document.getElementById('macdModal').style.display = 'block';
    }
}
function closeMacdModal() {
    const overlay = document.getElementById('macdModalOverlay');
    if (overlay) {
        overlay.style.display = 'none';
        document.getElementById('macdModal').style.display = 'none';
    }
}

function updatePeriodInputVisibility() {
    const indicatorSelect = document.getElementById('indicatorSelect');
    const periodInput = document.getElementById('periodInput');
    const showFor = ['SMA', 'EMA', 'RSI'];
    if (showFor.includes(indicatorSelect.value)) {
        periodInput.style.display = '';
    } else {
        periodInput.style.display = 'none';
    }
}

// Wait until DOM is loaded before attaching listeners
document.addEventListener('DOMContentLoaded', function () {
    const indicatorSelect = document.getElementById('indicatorSelect');
    if (!indicatorSelect) return; // safety check

    indicatorSelect.addEventListener('change', updatePeriodInputVisibility);
    updatePeriodInputVisibility(); // Set initial state
});

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

    if (!indicator || !['SMA', 'EMA', 'Volume', 'RSI', 'MACD', 'VWAP'].includes(indicator)) {
        alert("Select SMA, EMA, Volume, MACD, VWAP or RSI.");
        return;
    }

    if (indicator === 'MACD') {
        openMacdModal();
        return; // wait for confirmation
    }

    // VWAP doesn't require a period
    const key = indicator === 'VWAP' ? 'VWAP' : `${indicator}_${period}`;

    const response = await invokeCustomIndicator(indicator, period, validData);
    const result = await response.json();

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
            if (rsiChart && rsiChart.remove) {
                removeChartFromSync(rsiChart);
                rsiChart.remove();
            }
            rsiChart = null;
            rsiChart = LightweightCharts.createChart(rsiChartContainer, {
                width: chartWidth, height: 100,
                layout: { background: { color: '#fff' }, textColor: '#000' },
                grid: { vertLines: { color: '#eee' }, horzLines: { color: '#eee' } },
                crosshair: { mode: LightweightCharts.CrosshairMode.Normal },
                rightPriceScale: {
                    borderColor: '#ccc',
                    visible: true,
                    scaleMargins: { top: 0.05, bottom: 0.15 }
                },
                timeScale: {
                    visible: true,
                    timeVisible: interval !== 'day',
                    secondsVisible: false
                },
                localization: {
                    timeFormatter: (time) => indiaTimeFormatter(time, interval)
                }
            });
            registerChartForSync(rsiChart);
            requestAnimationFrame(() => {
                registerYAxisSync(mainChart);
                if (rsiChart) registerYAxisSync(rsiChart);
                if (macdChart) registerYAxisSync(macdChart);
            });
            const rsiSeries = rsiChart.addLineSeries({
                color,
                lineWidth: width,
            });
            rsiSeries.setData(data);
            indicatorSeriesMap[key] = { series: rsiSeries, chart: rsiChart, meta: { color, width } };
        } else {
            // VWAP, SMA, EMA go on main chart
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

    if (chart && typeof chart.timeScale === 'function') {
        try {
            chart.timeScale().unsubscribeVisibleTimeRangeChange(deferredYAxisSync);
        } catch (e) {
            console.warn('Failed to unsubscribe from time range sync:', e);
        }
    }

    const idx = syncedCharts.indexOf(chart);
    if (idx !== -1) {
        syncedCharts.splice(idx, 1);
    }

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
        if (typeof rsiChart !== 'undefined') {
            removeChartFromSync(rsiChart);
            rsiChart.remove();
            rsiChart = null;
        }

        const rsiChartContainer = document.getElementById('rsiChart');
        if (rsiChartContainer) rsiChartContainer.innerHTML = '';
    }

    if (key.startsWith('MACD')) {
        const macdChartContainer = document.getElementById('macdChart');
        if (macdChartContainer) macdChartContainer.innerHTML = '';
        if (typeof macdChart !== 'undefined') {
            removeChartFromSync(macdChart);
            macdChart.remove();
            macdChart = null;
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
        // Clean up sync handlers from syncedCharts array
        removeChartFromSync(rsiChart);
        rsiChart.remove();
        rsiChart = null;
    }

    if (macdChart) {
        removeChartFromSync(macdChart);
        macdChart.remove();
        macdChart = null;
    }
    renderIndicatorList();
}

async function invokeCustomIndicator(indicator, period, validData) {
    const open = validData.map(d => d.open);
    const high = validData.map(d => d.high);
    const low = validData.map(d => d.low);
    const close = validData.map(d => d.close);
    const volume = validData.map(d => d.volume);
    const timeStamp = validData.map(d => toUnixTime(d.time));
    const response = await fetch('/api/indicators/customIndicator', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ indicator, period, open, high, low, close, volume, timeStamp, interval })
    });
    return response;
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

async function applyMacd() {
    const macdShort = parseInt(document.getElementById('macdShort').value);
    const macdLong = parseInt(document.getElementById('macdLong').value);
    const macdSignal = parseInt(document.getElementById('macdSignal').value);

    if (macdShort >= macdLong) {
        alert("MACD Short EMA must be less than MACD Long EMA");
        return;
    }

    // Hide modal (entire overlay or just modal body as you prefer)
    const overlay = document.getElementById('macdModalOverlay');
    if (overlay) overlay.style.display = 'none';

    const open = validData.map(d => d.open);
    const high = validData.map(d => d.high);
    const low = validData.map(d => d.low);
    const close = validData.map(d => d.close);
    const volume = validData.map(d => d.volume);
    const timeStamp = validData.map(d => toUnixTime(d.time));
    const indicator = document.getElementById('indicatorSelect').value;

    const response = await fetch(`/api/indicators/customIndicator`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            indicator,
            macdShort,
            macdLong,
            macdSignal,
            open,
            high,
            low,
            close,
            volume,
            timeStamp,
            interval
        })
    });

    const result = await response.json();
    const macdKey = `MACD_${macdShort}_${macdLong}_${macdSignal}`;

    const macdData = result.macdValues.map((val, idx) => ({ time: validData[idx].time, value: val }));
    const signalData = result.signalValues.map((val, idx) => ({ time: validData[idx].time, value: val }));
    const histogramData = result.histogramValues.map((val, idx) => ({
        time: validData[idx].time,
        value: val,
        color: val >= 0 ? '#26a69a' : '#ef5350' // green for positive, red for negative
    }));

    // Remove old MACD if present
    if (indicatorSeriesMap[macdKey]) {
        const info = indicatorSeriesMap[macdKey];
        if (Array.isArray(info.series)) {
            info.series.forEach(s => info.chart.removeSeries(s));
        } else {
            info.chart.removeSeries(info.series);
        }
        delete indicatorSeriesMap[macdKey];
    }
    if (macdChart && macdChart.remove) {
        removeChartFromSync(macdChart);
        macdChart.remove();
        document.getElementById('macdChart').innerHTML = '';
        macdChart = null;
    }

    // Create new chart
    const chartWidth = document.getElementById('candlestickChart').clientWidth;
    const macdContainer = document.getElementById('macdChart');
    macdChart = LightweightCharts.createChart(macdContainer, {
        width: chartWidth,
        height: 100,
        layout: { background: { color: '#fff' }, textColor: '#000' },
        grid: { vertLines: { color: '#eee' }, horzLines: { color: '#eee' } },
        crosshair: { mode: LightweightCharts.CrosshairMode.Normal },
        rightPriceScale: {
            borderColor: '#ccc',
            visible: true,
            scaleMargins: { top: 0.05, bottom: 0.1 }
        },
        timeScale: {
            visible: true,
            timeVisible: interval !== 'day',
            secondsVisible: false
        },
        localization: {
            timeFormatter: (time) => indiaTimeFormatter(time, interval)
        }
    });

    const macdLine = macdChart.addLineSeries({ color: '#2196F3', lineWidth: 1 }); // blue
    const signalLine = macdChart.addLineSeries({ color: '#FF9800', lineWidth: 1 }); // orange
    const histogram = macdChart.addHistogramSeries({
        lineWidth: 1,
        priceFormat: { type: 'price' },
        scaleMargins: { top: 0.2, bottom: 0 },
    });

    macdLine.setData(macdData);
    signalLine.setData(signalData);
    histogram.setData(histogramData);

    indicatorSeriesMap[macdKey] = {
        series: [macdLine, signalLine, histogram],
        chart: macdChart,
        meta: { macdShort, macdLong, macdSignal }
    };

    registerChartForSync(macdChart);
    requestAnimationFrame(() => {
        registerYAxisSync(mainChart);
        if (rsiChart) registerYAxisSync(rsiChart);
        if (macdChart) registerYAxisSync(macdChart);
    });
    renderIndicatorList();
}
