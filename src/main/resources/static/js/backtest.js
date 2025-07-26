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
let interval = '';
var macdChart = null;

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

    const strategy = document.getElementById('strategySelect').value;
    const params = new URLSearchParams();
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    interval = document.getElementById('intervalSelect').value;

    params.append("equityId", selectedId.value);
    params.append("fromDate", startDate);
    params.append("toDate", endDate);
    params.append("interval", interval);

    if (strategy === 'historical') {
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
    } else if (strategy === 'ema-rsi') {
        lastStrategyParams = params;
        document.getElementById('emaRsiModal').style.display = 'block';
    }

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
            timeFormatter: (time) => indiaTimeFormatter(time, interval)
        }
    });
    registerChartForSync(mainChart);
    requestAnimationFrame(() => {
        registerYAxisSync(mainChart);
        if (rsiChart) registerYAxisSync(rsiChart);
        if (macdChart) registerYAxisSync(macdChart);
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
    document.getElementById('macdModalOverlay').style.display = 'none';
}

function plotBuySellMarkers(trades) {
    const markers = [];

    if (!Array.isArray(trades)) {
        console.error("Invalid trades data passed to plotBuySellMarkers:", trades);
        return;
    }

    trades.forEach(trade => {
        if (trade.buyTime && typeof trade.buyPrice === 'number') {
            markers.push({
                time: formatToChartTime(trade.buyTime),
                position: 'belowBar', // changed from 'below' to 'belowBar' for visibility
                color: 'green',
                shape: 'arrowUp',
                text: `Buy\n₹${trade.buyPrice.toFixed(2)}`,
                size: 1
            });
        }

        if (trade.sellTime && typeof trade.sellPrice === 'number') {
            let text = `Sell\n₹${trade.sellPrice.toFixed(2)}`;

            if (typeof trade.profit === 'number' && typeof trade.profitPercent === 'number') {
                const pnlColor = trade.profit >= 0 ? '🟢' : '🔴';
                text += `\n${pnlColor} ₹${trade.profit.toFixed(2)} (${trade.profitPercent.toFixed(2)}%)`;
            }

            markers.push({
                time: formatToChartTime(trade.sellTime),
                position: 'aboveBar', // changed from 'above' to 'aboveBar'
                color: 'red',
                shape: 'arrowDown',
                text: text,
                size: 1
            });
        }
    });

    if (mainCandleSeries && typeof mainCandleSeries.setMarkers === 'function') {
        mainCandleSeries.setMarkers(markers);
    } else {
        console.warn("mainCandleSeries is not ready. Cannot set markers.");
    }
}

window.onload = function () {
    initializeChartControls();

    document.getElementById('addIndicatorBtn').onclick = applyCustomIndicator;
    document.getElementById('clearIndicatorsBtn').onclick = clearAllIndicators;
    const closeBtn = document.getElementById('closeEmaRsiModal');
    const confirmBtn = document.getElementById('confirmEmaRsi');
    //const macdCloseBtn = document.getElementById('closeMacdModal');
    //const macdConfirmBtn = document.getElementById('confirmMacdBtn');

    /*if (macdCloseBtn) {
        macdCloseBtn.onclick = () => {
            document.getElementById('macdModal').style.display = 'none';
        };
    }*/

    /*if (macdConfirmBtn) {
        macdConfirmBtn.onclick = async () => {
            const macdShort = parseInt(document.getElementById('macdShort').value);
            const macdLong = parseInt(document.getElementById('macdLong').value);
            const macdSignal = parseInt(document.getElementById('macdSignal').value);

            if (macdShort >= macdLong) {
                alert("MACD Short EMA must be less than MACD Long EMA");
                return;
            }

            document.getElementById('macdModal').style.display = 'none';
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
            const histogramData = result.histogramValues.map((val, idx) => ({ time: validData[idx].time, value: val }));

            // Remove old MACD chart if any
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

            // Create new MACD chart
            const chartWidth = document.getElementById('candlestickChart').clientWidth;
            const macdContainer = document.getElementById('macdChart');

            macdChart = LightweightCharts.createChart(macdContainer, {
                width: chartWidth,
                height: 100,
                layout: { background: { color: '#fff' }, textColor: '#000' },
                grid: { vertLines: { color: '#eee' }, horzLines: { color: '#eee' } },
                crosshair: { mode: LightweightCharts.CrosshairMode.Normal },
                rightPriceScale: {
                    visible: true,
                    scaleMargins: { top: 0.2, bottom: 0.1 }
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

            const macdLine = window.macdChart.addLineSeries({ color: '#2196F3', lineWidth: 1 }); // blue
            const signalLine = window.macdChart.addLineSeries({ color: '#FF9800', lineWidth: 1 }); // orange
            const histogram = window.macdChart.addHistogramSeries({
                color: '#9E9E9E',
                lineWidth: 1,
                priceFormat: { type: 'price' },
                scaleMargins: { top: 0.2, bottom: 0 },
            });

            macdLine.setData(macdData);
            signalLine.setData(signalData);
            histogram.setData(histogramData);

            indicatorSeriesMap[macdKey] = {
                series: [macdLine, signalLine, histogram],
                chart: window.macdChart,
                meta: { macdShort, macdLong, macdSignal }
            };

            registerChartForSync(macdChart);
            requestAnimationFrame(() => registerYAxisSync(macdChart));
            renderIndicatorList();
        };
    }*/

    if (closeBtn) {
        closeBtn.onclick = () => {
            document.getElementById('emaRsiModal').style.display = 'none';
        };
    }

    if (confirmBtn) {
        confirmBtn.onclick = () => {
            const emaShort = document.getElementById('emaShort').value;
            const emaLong = document.getElementById('emaLong').value;
            const rsiPeriod = document.getElementById('rsiPeriod').value;
            const entryRsiThreshold = document.getElementById('entryRsiThreshold').value;
            const stopLossPercent = document.getElementById('stopLossPercent').value;
            const takeProfitPercent = document.getElementById('takeProfitPercent').value;
            const useTrailingStopLoss = document.getElementById('trailingStopLossCheckbox').checked;

            document.getElementById('emaRsiModal').style.display = 'none';

            fetch(`/api/backtest/ema-rsi`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    equityId: lastStrategyParams.get("equityId"),
                    fromDate: lastStrategyParams.get("fromDate"),
                    toDate: lastStrategyParams.get("toDate"),
                    interval: lastStrategyParams.get("interval"),
                    emaShortPeriod: parseInt(emaShort),
                    emaLongPeriod: parseInt(emaLong),
                    rsiPeriod: parseInt(rsiPeriod),
                    entryRsiThreshold: parseInt(entryRsiThreshold),
                    stopLossPercent: parseFloat(stopLossPercent),
                    takeProfitPercent: parseFloat(takeProfitPercent),
                    useTrailingStopLoss : useTrailingStopLoss
                })
            })
                .then(response => {
                if (!response.ok) throw new Error("Strategy response was not ok");
                return response.json();
            })
                .then(response => {
                currentChartData = response.historicalData;
                renderCandlestickChart(response.historicalData, lastStrategyParams.get("interval"));
                plotBuySellMarkers(response.trades);
                populateTradeTable(response.trades, lastStrategyParams.get("interval"));
            })
                .catch(err => {
                console.error("Error fetching ema-rsi strategy:", err);
            });
        };
    }
};

function populateTradeTable(trades, interval) {
    const tbody = document.querySelector("#tradeSummaryTable tbody");
    const totalProfitCell = document.getElementById("totalProfit");
    const totalLossCell = document.getElementById("totalLoss");
    const totalCell = document.getElementById("totalPnL");

    tbody.innerHTML = ""; // Clear existing rows
    let totalProfit = 0;
    let totalLoss = 0;
    let total = 0;

    trades.forEach((trade, index) => {
        const row = document.createElement("tr");
        const profit = trade.profit;

        if (profit >= 0) {
            totalProfit += profit;
        } else {
            totalLoss += profit;
        }
        total += profit;
        row.innerHTML = `
            <td>${index + 1}</td>
            <td>${formatDateTime(trade.buyTime, interval)}</td>
            <td>₹${trade.buyPrice.toFixed(2)}</td>
            <td>${formatDateTime(trade.sellTime, interval)}</td>
            <td>₹${trade.sellPrice.toFixed(2)}</td>
            <td>${formatSellType(trade.sellType)}</td>
            <td class="${profit >= 0 ? 'profit' : 'loss'}">₹${profit.toFixed(2)}</td>
            <td class="${trade.profitPercent >= 0 ? 'profit' : 'loss'}">${trade.profitPercent.toFixed(2)}%</td>
        `;
        tbody.appendChild(row);
    });

    totalProfitCell.innerHTML = `<strong class="profit">₹${totalProfit.toFixed(2)}</strong>`;
    totalLossCell.innerHTML = `<strong class="loss">₹${totalLoss.toFixed(2)}</strong>`;
    totalCell.innerHTML = `<strong class="${total >= 0 ? 'profit' : 'loss'}">₹${total.toFixed(2)}</strong>`;

    document.getElementById('tradeTableContainer').style.display = 'block';
}