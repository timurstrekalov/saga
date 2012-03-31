function Class(param) {
    var a = 1;
    var b = a + 122;
    var c = {
        a: a,
        b: b
    };

    function asdd() {
        this.asdd = 'qwe';
    };

    a++;
    c.a++;
    b["b"]++;

    var d = {
        a: function () {
            this.b = "qwe";
        }
    };

    var e = function () {
        this.e = "aaaaa";
    };

    if (param) {
        this.test();
    }

    this.data = {
        handleMe: function () {
            var a = 'lll';
            a += 1;

            if (param) {
                a += 2;
            } else {
                a = 'kkk';
            }
        },

        a: 123
    }
}

Class.prototype.test = function test() {
    this.qwe = 'asd';
    var d = 111;

    this.qqq = 'bbb';
}