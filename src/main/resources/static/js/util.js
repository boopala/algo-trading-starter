function toUnixTime(time) {
    if (typeof time === 'object' && time.year) {
        // Convert BusinessDay to UNIX timestamp
        const date = new Date(time.year, time.month - 1, time.day); // Month is 0-based
        return Math.floor(date.getTime() / 1000).toString();
    }
    return time.toString();
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

function formatToChartTime(dateString) {
    const date = new Date(dateString);
    return {
        year: date.getUTCFullYear(),
        month: date.getUTCMonth() + 1,
        day: date.getUTCDate(),
        hour: date.getUTCHours(),
        minute: date.getUTCMinutes()
    };
}

function formatDateTime(dateStr, interval) {
    const date = new Date(dateStr);

    if (interval === "day") {
        // Return only the date
        return date.toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        });
    } else {
        return date.toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        }) + ', ' + date.toLocaleTimeString('en-IN', {
            hour: '2-digit',
            minute: '2-digit',
            hour12: true
        });
    }
}

function indiaTimeFormatter(time, interval) {
    if (typeof time === 'object' && time.year) {
        // BusinessDay format for day interval
        const { year, month, day } = time;
        return `${String(day).padStart(2, '0')}-${String(month).padStart(2, '0')}-${year}`;
    } else {
        // Unix timestamp (in seconds)
        const date = new Date(time * 1000);
        return date.toLocaleString('en-IN', {
            timeZone: 'Asia/Kolkata',
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: interval === 'day' ? undefined : '2-digit',
            minute: interval === 'day' ? undefined : '2-digit',
            hour12: true
        });
    }
}

function formatSellType(type) {
    switch (type) {
        case "STRATEGY_SELL": return "STRATEGY_SELL";
        case "STOP_LOSS_SELL": return "STOP_LOSS_SELL";
        case "TAKE_PROFIT_SELL": return "TAKE_PROFIT_SELL";
        case "FINAL_CLOSE": return "FINAL_CLOSE";
        default: return "-";
    }
}