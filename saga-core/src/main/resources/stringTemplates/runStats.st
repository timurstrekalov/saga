runStats(stats, name, version, url) ::= <<
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

    .stats-header th {
        color: white;
    }

    .stats-header th {
        text-align: right;
    }
    
    .stats-header th:first-child {
        text-align: left;
    }

    .stats td.statements,
    .stats td.executed,
    .stats td.coverage {
        text-align: right;
    }

    .stats > thead th,
    .stats > thead td,
    .stats > tbody > tr > td {
        padding: 4px 10px;
    }

    .stats > thead th {
        font-size: 0.85em;
    }

    .stats > thead > tr:first-child th {
        background-color: #758691;
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
        background-color: #e7e8e9;
    }

    .stats > tbody > tr:nth-child(2n + 1) {
        cursor: pointer;
    }

    .stats > tbody > tr:nth-child(2n + 1):hover {
        background-color: #b1c7d4;
    }

    .stats tr.no-statements {
        opacity: 0.6;
    }

    a, a:hover, a:visited, a:active {
        color: black;
        text-decoration: none;
    }

    .bar-container {
        padding: 2px;
        padding-right: 8px;
        width: 15%;
    }

    .bar {
        background-color: #DDD;
        background-color: rgba(0, 0, 0, 0.1);
        border-radius: 8px 8px 8px 8px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1) inset, 0 1px 0 rgba(255, 255, 255, 0.5);
        height: 7px;
        padding: 1px;
        position: relative;
        width: 100%;
    }

    .bar > span {
        background-color: #AEDB4B;
        border-radius: 6px 6px 6px 6px;
        box-shadow: 0 -2px 2px rgba(0, 0, 0, 0.2) inset, 0 1px 0 rgba(255, 255, 255, 0.4) inset, 0 -1px 0 rgba(0, 0, 0, 0.15) inset;
        display: block;
        height: 100%;
    }

    .bar > span:before {
        position: absolute;
        width: 25%;
        height: 100%;
        left: 0;
        top: 0;
        border-right: 1px solid rgba(0,0,0,0.1);
        content: ' ';
    }
    .bar > span:after {
        position: absolute;
        width: 25%;
        height: 100%;
        left: 50%;
        top: 0;
        border-left: 1px solid rgba(0,0,0,0.1);
        border-right: 1px solid rgba(0,0,0,0.1);
        content: ' ';
    }

    /* line by line coverage styles */

    .line-coverage {
        margin: 10px 0px;
    }

    .line-coverage th {
        border-right: 1px solid #758691;
    }

    .line-coverage th div,
    .line-coverage td div {
        text-align: center;
    }

    .line-coverage th div {
        margin: 0px 10px 0px 4px;
    }

    .line-coverage td div {
        padding: 2px 6px 2px 4px;
        margin: 0;
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
        border-radius: 0 5px 5px 0;
        color: rgba(0, 0, 0, 0.8);
        font-size: 0.9em;
    }

    .line-coverage td pre {
        margin-left: 8px;
    }

    .line-coverage tr.covered td.times-executed div {
        background-color: #aedb4b;
    }

    .line-coverage tr.not-covered td.times-executed div {
        background-color: #ff8134;
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

    .line-coverage a {
        color: black;
    }

    .line-coverage-cell > div {
        color: #FF8134;
    }

    .line-coverage-cell > div > a {
        color: #FF8134;
        font-weight: bold;
        text-decoration: underline;
    }

    .stats > thead td {
        font-weight: bold;
    }

    .footer {
        padding-top: 5px;
        text-align: right;
        font-style: italic;
        font-size: 0.85em;
        opacity: 0.7;
    }

    .footer a {
        text-decoration: underline;
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

        String.prototype.reverse = function () {
            return this.split('').reverse().join('');
        };

        var reservedKeywordsPattern = new RegExp('\\\b(' + reservedKeywords.join('|') + ')\\\b', 'g');
        var jsStringPattern = /('(?!\\\').*'|"(?!\\\").*")/g;
        var jsNumberPattern = /\b(\d+(?:\.\d+)?)\b/g;

        function el(type) {
            return document.createElement(type);
        }

        function withText(node, text) {
            node.innerHTML = text;
            return node;
        }

        function maybeAppendMissedLines(id, frag, missed, data) {
            if (missed.length === 0) {
                return;
            }

            var rangeAnchors = [];
            var range;

            for (var i = 0, len = missed.length; i < len; i++) {
                var start = missed[i];
                var end = start;

                range = [start];

                for (var j = i + 1; j < len; j++) {
                    var missedLineNr = missed[j];
                    var consecutive = false;

                    // consecutive missed lines
                    if (missedLineNr === end + 1) {
                        consecutive = true;
                    } else {
                        // let's check if all lines in between are not executable
                        var dataIdx = end;

                        while (dataIdx < missedLineNr) {
                            if (data[dataIdx][0] > -1) {
                                if (data[dataIdx][0] === 0) {
                                    missedLineNr = dataIdx + 1;
                                    consecutive = true;
                                }

                                break;
                            }

                            dataIdx++;
                        }
                    }

                    if (consecutive) {
                        end = missedLineNr;
                        i = j;
                    }
                }

                range[1] = end;

                var rangeStr = start === end ? start : start + '-' + end;
                rangeAnchors.push('<a href="' + '#' + id + '-' + start + '">' + rangeStr + '</a>');
            }

            frag.appendChild(withText(el('div'), 'Missed lines: ' + rangeAnchors.join(', ')));
        }

        function createSourceTable(id, dataEl) {
            var d = document;

            var data = eval(dataEl.innerHTML);
            var td = dataEl.parentNode;
            td.removeChild(dataEl);

            var frag = d.createDocumentFragment();

            var table = el('table');
            table.className = 'line-coverage';

            var missed = [];

            for (var i = 0, len = data.length; i < len; i++) {
                var tr = el('tr');
                var record = data[i];

                var timesExecuted = record[0];
                var src = record[1];
                var executable = timesExecuted > -1;
                var lineNr = i + 1;

                if (timesExecuted === 0) {
                    missed.push(lineNr);
                }

                tr.className = !executable ? 'not-executable' : timesExecuted > 0 ? 'covered' : 'not-covered';
                table.appendChild(tr);

                var lineNrNode = el('th');

                var anchor = el('a');
                anchor.setAttribute('name', id + '-' + lineNr);
                anchor.appendChild(withText(el('div'), lineNr));

                lineNrNode.appendChild(anchor);
                tr.appendChild(lineNrNode);

                var timesExecutedNode = el('td');
                timesExecutedNode.className = 'times-executed';
                timesExecutedNode.appendChild(withText(el('div'), executable ? (timesExecuted || '!') : '&nbsp;'));
                tr.appendChild(timesExecutedNode);

                var srcNode = el('td');

                var styledSrc = src
                    .reverse().replace(jsStringPattern, '>naps/<\$1>"gnirts"=ssalc naps<').reverse()
                    .replace(jsNumberPattern, '<span class="number">\$1</span>')
                    .replace(reservedKeywordsPattern, '<span class="keyword">\$1</span>');

                srcNode.appendChild(withText(el('pre'), styledSrc));

                tr.appendChild(srcNode);
            }

            maybeAppendMissedLines(id, frag, missed, data);
            frag.appendChild(table);

            td.appendChild(frag.cloneNode(true));
        }

        window.__coverage_toggleSection = function (hash) {
            var d = document;
            var id = hash.indexOf('-') > -1 ? hash.substring(0, hash.indexOf('-')) : hash;

            var srcRow = d.getElementById('__src_' + id);
            if (!srcRow) {
                return;
            }

            var dataEl = d.getElementById('__data_' + id);

            if (dataEl) {
                createSourceTable(id, dataEl);
            }

            var show = srcRow.style.display === 'none';

            if (show) {
                srcRow.style.display = '';
                window.location.hash = "#" + hash;
            } else {
                srcRow.style.display = 'none';
                window.location.hash = '#closed';
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
            <tr class="stats-header $if(!stats.hasStatements)$no-statements$endif$">
                <td>Total</td>
                <td class="statements">$stats.totalStatements$</td>
                <td class="executed">$stats.totalExecuted$</td>
                <td class="coverage">
                    $if(stats.hasStatements)$
                        $stats.totalCoverage$&thinsp;%
                    $else$
                        -
                    $endif$
                </td>
                <td class="bar-container">
                    $if(stats.hasStatements)$
                        <div class="bar">
                            <span style="width: $stats.totalCoverage$%; background-color: rgb($stats.barColor$);"></span>
                        </div>
                    $endif$
                </td>
            </tr>
        </thead>
        <tbody>
            $stats.fileStats:fileStats()$
        </tbody>
    </table>
    <div class="footer">Generated using <a href="$url$">$name$</a> version $version$</div>
</body>
</html>
>>