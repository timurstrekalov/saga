/**
 * Multiline comment just in case: used to be a bug
 */
 // or any comment, for that matter
(function () {

    'use strict';

    window.Class = function (param) {
        var a = 1;
        var b = a + 122;
        var c = {
            a: a,
            b: b,
            g: undefined,
            j: null
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

        var m1 = {},
            m2 = null,
            m3 = 'asd',
            m4 = 'qwe';

        if (param)
            a++;
        else
            b++

        if (!param)
            b++;
        else
            a++

        while (true === false)
            b++

        var s = 1;

        while (++s < 10)
            s++

        for (var j = 0; j < 20; j++)
            s += 2;

        for (var k = 0; k < 2; k++) s+=1

        do
            s += 3;
        while (s < 70)

        var v = void 0;
        var vv = void 0xff;
        var vvv = void(0);

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

        if (false) {
            // testing consecutive ranges of missed lines
            a++;
            b++;
            z++;

            a++;
            b++;

            var h = 'qwe';
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
                case 4:
                case 5:
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

        var h = 0;

        if (a)
            if (z)
                for (h = 0; h < 10; h++) {
                    a++;
                }
            else
                for (h = 0; h < 10; h++) {
                    if (h == 5) break
                }
        else
            if (z)
                while (h++ < 10);
            else
                for (h = 0; h < 10; h++);

        if (param) {a++;if (!param)if (true)if(true){a++;b++;}else a++;else{b++;z++}a+=2;}
    }

})();

Class.prototype.test = function test() {
    this.qwe = 'asd';
    var d = 111;

    this.qqq = 'bbb';
}