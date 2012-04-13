runStats(stats) ::= <<
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Coverage report for run "$stats.runName$"</title>
<style>
    body {
        margin: 0;
        padding: 18px 10px;
        font: 0.8125em Verdana, sans-serif;
    }

    h1 {
        font-size: 1.2em;
        margin: 0;
        padding-left: 10px;
        padding-bottom: 5px;
    }

    table {
        border-collapse: collapse;
    }

    .stats {
        width: 100%;
    }

    .stats-header th,
    .stats td.statements,
    .stats td.executed,
    .stats td.coverage {
        text-align: center;
    }

    .stats > thead th,
    .stats > thead td,
    .stats > tbody > tr > td {
        padding: 4px 10px;
    }

    .stats > tbody > tr > td.line-coverage-cell {
        padding: 0px;
    }

    .stats > thead th {
        font-size: 1.1em;
    }

    .stats > thead tr:first-child {
        background-color: #ccc;
    }

    .stats > thead th:first-child,
    .stats > thead td:first-child,
    .stats > tbody > tr > td:first-child {
        border-top-left-radius: 4px;
        border-bottom-left-radius: 4px;
    }

    .stats > thead th:last-child,
    .stats > thead td:last-child,
    .stats > tbody > tr > td:last-child {
        border-top-right-radius: 4px;
        border-bottom-right-radius: 4px;
    }

    .stats > tbody > tr:nth-child(4n + 1) {
        background-color: #eee;
    }

    a, a:hover, a:visited, a:active {
        color: #0000ff;
    }

    .bar-container {
        padding: 2px;
        padding-right: 8px;
        width: 15%;
    }

    .bar {
        border-radius: 8px;
        background-color: #555;

        padding: 2px;

        position: relative;
        height: 16px;
        width: 100%;
    }

    .bar > span {
        background-color: #00dd00;

        border-radius: 6px;

        display: block;
        height: 100%;
    }

    /* line by line coverage styles */

    .line-coverage {
        margin: 10px 0px;
    }

    .line-coverage th {
        border-right: 1px solid #ccc;
    }

    .line-coverage th div,
    .line-coverage td div {
        text-align: center;
    }

    .line-coverage th div {
        margin: 0px 10px 0px 4px;
    }

    .line-coverage td div {
        padding: 1px 6px;
        margin: 0px 8px;
    }

    .line-coverage th,
    .line-coverage td,
    .line-coverage pre {
        padding: 0;
        margin: 0;

        font-size: 1.05em;
        font-weight: normal;
        font-family: Courier New;
    }

    .line-coverage th div,
    .line-coverage td.times-executed div {
         border-radius: 8px;
    }

    .line-coverage td pre {
         margin-left: 8px;
    }

    .line-coverage tr.covered td.times-executed div {
         background-color: lightgreen;
    }

    .line-coverage tr.not-covered td.times-executed div {
         background-color: lightpink;
    }

    .line-coverage span.keyword {
        color: #000080;
        font-weight: bold;
    }

    .line-coverage span.string {
        color: #008000;
    }

    .line-coverage span.number {
        color: #0000ff;
    }
</style>
<script type="text/javascript">
    function __coverage_toggleSection(id) {
        var el = document.getElementById('__src_' + id);
        if (!el) {
            return;
        }

        var show = el.style.display === 'none';

        if (show) {
            el.style.display = '';
            window.location.hash = "#" + id;
        } else {
            el.style.display = 'none';
            window.location.hash = '';
        }
    }

    window.onload = function () {
        if (window.location.hash) {
            __coverage_toggleSection(window.location.hash.substr(1));
        }
    };
</script>
</head>
<body>
    <h1>Coverage report for run "$stats.runName$"</h1>
    <table class="stats">
        <thead>
            <tr class="stats-header">
                <th>File</th>
                <th>Statements</th>
                <th>Executed</th>
                <th>Coverage</th>
                <th></th>
            </tr>
            <tr class="stats-header">
                <td>Total</td>
                <td class="statements">$stats.totalStatements$</td>
                <td class="executed">$stats.totalExecuted$</td>
                <td class="coverage">$stats.totalCoverage$%</td>
                <td class="bar-container">
                    <div class="bar">
                        <span style="width: $stats.totalCoverage$%;"></span>
                    </div>
                </td>
            </tr>
        </thead>
        <tbody>
            $stats.fileStats:fileStats()$
        </tbody>
    </table>
</body>
</html>
>>