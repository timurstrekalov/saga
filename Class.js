'use strict';

function Class(param) {
    var a = 1;
    var b = a + 122;
    var c = {
        a: a,
        b: b
    };

    var z = (function (param) {
        var y = a + b;
        delete b['z'];
        return param ? y : 0;
    })(param);

    for(i=2;i<4;i++){i++;}

    function thisNeverGetsCalled() {
        this.shouldNeverBeSet = 'qwe \'';
    };

    (function (param) {
        a++;

        if (param) {
            b--;
        }
    })(param);

    a++;
    c.a++;
    b["b"]++;

    var uselessObject = {
        neverGetsCalledEither: function () {
            this.b = "q \" we";
        },

        butThisOneDoes: function () {
            a++;
        },

        andThisOneButItsUgly: function () {a++;},

        andThisOneButItsEmpty: function () {}
    };

    var e = function () {
        this.e = "aaaaa";
    };

    if (param) {
        this.test();
    }

    this.data = {
        getsCalledOnTheSecondRun: function () {
            var a = 'lll';
            a += 1;

            if (param === true) {
                a += 2;
            } else if (typeof param === 'boolean') {
                a += 10;
            } else if (param !== undefined) {
                b += 7;
            } else {
                a = 'kkk';
            }

            if (param) {
                b++;
            } else {
                a++;
            }
        },

        a: 123
    }

    for (var i = 0; i < 25; i++) {
        b++;

        switch (i) {
            case 1:
                break;
            case 2:
                a++;
                b++;
                break;
            case 3:
                b++;
            default:
                b++;
        }

        if (i === 4) {
            break;
        }
    }

    do {
        a++;
    } while (a < 200);

    while (true) {
        b++;
        break;
    }

    try {
        a++;
        throw new Error('asd');
    } catch (e) {
        b++;
    }

    if (param) {
        uselessObject.butThisOneDoes();
        uselessObject.andThisOneButItsUgly();
        uselessObject.andThisOneButItsEmpty();
    }
}

Class.prototype.test = function test() {
    this.qwe = 'asd';
    var d = 111;

    this.qqq = 'bbb';
}