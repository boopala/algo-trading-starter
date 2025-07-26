const syncedCharts = [ ];  // Holds all charts to sync
let syncPending = false;

function syncAllYAxisWidths() {
    if (!Array.isArray(syncedCharts)) return;

    // Filter valid charts with timeScale and priceScale
    const validCharts = syncedCharts.filter(chart =>
    chart && typeof chart.priceScale === 'function' && chart.priceScale('right')
    );

    // Update syncedCharts to only include valid ones
    syncedCharts = validCharts;

    if (validCharts.length === 0) return;

    const widths = validCharts.map(chart => {
        const ps = chart.priceScale('right');
        return ps && typeof ps.width === 'function' ? ps.width() : 0;
    });

    const maxWidth = Math.max(...widths);

    validCharts.forEach(chart => {
        const ps = chart.priceScale('right');
        if (ps && typeof ps.setWidth === 'function') {
            ps.setWidth(maxWidth);
        }
    });
}


function deferredYAxisSync() {
    if (!syncPending) {
        syncPending = true;
        requestAnimationFrame(() => {
            syncAllYAxisWidths();
            syncPending = false;
        });
    }
}

function registerYAxisSync(chart) {
    if (!syncedCharts.includes(chart)) {
        syncedCharts.push(chart);
        chart.timeScale().subscribeVisibleTimeRangeChange(deferredYAxisSync);
        chart.timeScale().subscribeSizeChange(deferredYAxisSync);
    }
}

function registerChartForSync(chart) {
    if (!chart || syncedCharts.includes(chart)) return;

    syncedCharts.push(chart);

    chart.timeScale().subscribeVisibleTimeRangeChange(() => {
        const range = chart.timeScale().getVisibleRange();
        syncedCharts.forEach(c => {
            if (c !== chart &&
            c.timeScale &&
            typeof c.timeScale === 'function' &&
            c.timeScale().setVisibleRange &&
            c.timeScale().getVisibleRange() !== null) {
                if (range && typeof range.from !== 'undefined' && typeof range.to !== 'undefined') {
                    c.timeScale().setVisibleRange(range);
                }
            }
        });
    });

    chart.timeScale().subscribeVisibleLogicalRangeChange(() => {
        const logical = chart.timeScale().getVisibleLogicalRange();
        syncedCharts.forEach(c => {
            if (c !== chart) {
                if (
                logical &&
                typeof logical.from === 'number' &&
                typeof logical.to === 'number' &&
                !isNaN(logical.from) &&
                !isNaN(logical.to)
                ) {
                    c.timeScale().setVisibleLogicalRange(logical);
                } else {
                    console.warn("Skipped syncing chart due to invalid logical range:", logical);
                }
            }
        });
    });
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

    function resizeCharts() {
        const chartDiv = document.getElementById('candlestickChart');
        const rsiDiv = document.getElementById('rsiChart');
        const macdDiv = document.getElementById('macdChart');
        const isFullScreen = document.fullscreenElement === chartContainer;
        const width = chartContainer.clientWidth;

        if (isFullScreen) {
            const screenHeight = window.innerHeight;
            const spacing = 2; // Gap between charts

            const hasRSI = rsiDiv && rsiDiv.childElementCount > 0;
            const hasMACD = macdDiv && macdDiv.childElementCount > 0;

            let totalCharts = 1 + (hasRSI ? 1 : 0) + (hasMACD ? 1 : 0);
            let availableHeight = screenHeight - ((totalCharts - 1) * spacing);

            // Safe fallback
            if (availableHeight <= 0) availableHeight = screenHeight;

            const mainHeight = hasRSI && hasMACD
            ? Math.floor(availableHeight * 0.65)
            : hasRSI || hasMACD
            ? Math.floor(availableHeight * 0.75)
            : availableHeight;

            const rsiHeight = hasRSI ? Math.floor((availableHeight - mainHeight) / (hasMACD ? 2 : 1)) : 0;
            const macdHeight = hasMACD ? (availableHeight - mainHeight - (hasRSI ? rsiHeight : 0)) : 0;

            // Set heights with spacing
            chartDiv.style.height = `${mainHeight}px`;
            if (mainChart) mainChart.resize(width, mainHeight);

            if (hasRSI) {
                rsiDiv.style.marginTop = `${spacing}px`;
                rsiDiv.style.height = `${rsiHeight}px`;
                if (rsiChart) rsiChart.resize(width, rsiHeight);
            }

            if (hasMACD) {
                macdDiv.style.marginTop = `${spacing}px`;
                macdDiv.style.height = `${macdHeight}px`;
                if (macdChart) macdChart.resize(width, macdHeight);
            }
        } else {
            // Exit fullscreen – reset layout
            chartDiv.style.width = '100%';
            chartDiv.style.height = '400px';
            if (mainChart) mainChart.resize(width, 400);

            if (rsiDiv) {
                rsiDiv.style.marginTop = '2px';
                rsiDiv.style.height = '100px';
                rsiDiv.style.minHeight = '100px';
                rsiDiv.style.display = 'block';
                if (rsiChart) rsiChart.resize(width, 100);
            }

            if (macdDiv) {
                macdDiv.style.marginTop = '2px';
                macdDiv.style.height = '100px';
                macdDiv.style.minHeight = '100px';
                macdDiv.style.display = 'block';
                if (macdChart) macdChart.resize(width, 100);
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

function clearChartOnNewInterval() {
    // Remove all indicator series
    clearAllIndicators();
    if (mainChart) {
        removeChartFromSync(mainChart);
        mainChart.remove();
        mainChart = null; // Allow clean re-creation
    }
}

function removeChartFromSync(chart) {
    const index = syncedCharts.indexOf(chart);
    if (index !== -1) {
        syncedCharts.splice(index, 1);
    }
}
