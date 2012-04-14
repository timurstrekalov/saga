runStats(stats) ::= <<
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>$stats.title$</title>
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
    (function () {
        var reservedKeywords = [
            "break",
            "case",
            "catch",
            "continue",
            "debugger",
            "default",
            "delete",
            "do",
            "else",
            "false",
            "finally",
            "for",
            "function",
            "if",
            "in",
            "instanceof",
            "new",
            "null",
            "return",
            "switch",
            "this",
            "true",
            "throw",
            "try",
            "typeof",
            "var",
            "void",
            "while",
            "with"
        ];

        var reservedKeywordsPattern = new RegExp('\\\b(' + reservedKeywords.join('|') + ')\\\b', 'g');
        var jsStringPattern = /('.*'|".*")/g;
        var jsNumberPattern = /\b(\d+(?:\.\d+)?)\b/g;

        console.log('\\b(' + reservedKeywords.join('|') + ')\\b');

        function el(type) {
            return document.createElement(type);
        }

        function withText(node, text) {
            node.innerHTML = text;
            return node;
        }

        function createSourceTable(dataEl) {
            var d = document;

            var data = eval(dataEl.innerHTML);
            var td = dataEl.parentNode;
            td.removeChild(dataEl);

            var frag = d.createDocumentFragment();

            var table = el('table');
            table.className = 'line-coverage';
            frag.appendChild(table);

            for (var i = 0, len = data.length; i < len; i++) {
                var tr = el('tr');
                var record = data[i];

                var executable = !!record[0];
                var timesExecuted = record[1];
                var src = record[2];

                tr.className = !executable ? 'not-executable' : timesExecuted > 0 ? 'covered' : 'not-covered';
                table.appendChild(tr);

                var lineNrNode = el('th');
                lineNrNode.appendChild(withText(el('div'), i + 1));
                tr.appendChild(lineNrNode);

                var timesExecutedNode = el('td');
                timesExecutedNode.className = 'times-executed';
                timesExecutedNode.appendChild(withText(el('div'), executable ? timesExecuted : '&nbsp;'));
                tr.appendChild(timesExecutedNode);

                var srcNode = el('td');

                var styledSrc = src
                    .replace(jsStringPattern, '<span class="string">\$1</span>')
                    .replace(jsNumberPattern, '<span class="number">\$1</span>')
                    .replace(reservedKeywordsPattern, '<span class="keyword">\$1</span>');

                srcNode.appendChild(withText(el('pre'), styledSrc));

                tr.appendChild(srcNode);
            }

            td.appendChild(frag.cloneNode(true));
        }

        window.__coverage_toggleSection = function (id) {
            var d = document;
            var srcRow = d.getElementById('__src_' + id);
            if (!srcRow) {
                return;
            }

            var dataEl = d.getElementById('__data_' + id);

            if (dataEl) {
                createSourceTable(dataEl);
            }

            var show = srcRow.style.display === 'none';

            if (show) {
                srcRow.style.display = '';
                window.location.hash = "#" + id;
            } else {
                srcRow.style.display = 'none';
                window.location.hash = '';
            }
        }

        window.onload = function () {
            if (window.location.hash) {
                __coverage_toggleSection(window.location.hash.substr(1));
            }
        };

    })();
</script>
</head>
<body>
    <h1>$stats.title$</h1>
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