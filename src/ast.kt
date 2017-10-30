
interface Exp {
    var tipo: String?
}

interface Cmd

fun <T> duplicadas(l: List<T>): Set<T> {
    val s = mutableSetOf<T>()
    val dups = mutableSetOf<T>()
    for(item in l) {
        if(s.contains(item)) dups.add(item);
        else s.add(item);
    }
    return dups;
}

data class Tiny(val procs: List<Proc>, val corpo: Bloco) {
    fun escopo(): List<String> {
        val erros = mutableListOf<String>()
        // Primeira passada
        val dvars = duplicadas(corpo.vars)
        for(v in dvars) {
            erros.add("variável global $v declarada mais de uma vez")
        }
        val vars =
                (corpo.vars.map { v -> Pair(v.nome, corpo) }).toMap()
        val dprocs = duplicadas(procs.map { p -> p.nome })
        for(proc in dprocs) {
            erros.add("procedimento $proc declarado mais de uma vez")
        }
        val procs =
                (procs.map { p -> Pair(p.nome, p) }).toMap()
        // Segunda passada
        for(proc in this.procs)
            escopo(proc.corpo, procs, vars, erros)
        for(cmd in corpo.cmds)
            escopo(cmd, procs, vars, erros)
        return erros
    }
}

data class Bloco(val vars: List<Decl>,
                 val cmds: MutableList<Cmd>, val lin: Int) {
    fun add(cmd: Cmd) {
        cmds.add(cmd)
    }
}

data class Decl(val nome: String, val tipo: String)

data class Proc(val nome: String, val corpo: Bloco, val lin: Int)

data class Chamada(val nome: String, val lin: Int): Cmd {
    var proc: Proc? = null
}

data class If(val cond: Exp, val th: Bloco, val els: Bloco, val lin: Int) : Cmd

data class Repeat(val corpo: Bloco, val cond: Exp,
                  val lin: Int): Cmd

data class Atrib(val lval: String, val rval: Exp,
                 val lin: Int): Cmd {
    var escopo: Bloco? = null
}

data class Read(val lval: String, val lin: Int): Cmd {
    var escopo: Bloco? = null
}

data class Write(val exp: Exp, val lin: Int): Cmd

data class Menor(val e1: Exp, val e2: Exp, val lin: Int): Exp {
    override var tipo: String? = null
}

data class Igual(val e1: Exp, val e2: Exp, val lin: Int): Exp {
    override var tipo: String? = null
}

data class Soma(val e1: Exp, val e2: Exp, val lin: Int): Exp {
    override var tipo: String? = null
}

data class Sub(val e1: Exp, val e2: Exp, val lin: Int): Exp {
    override var tipo: String? = null
}

data class Mult(val e1: Exp, val e2: Exp, val lin: Int): Exp {
    override var tipo: String? = null
}

data class Div(val e1: Exp, val e2: Exp, val lin: Int): Exp {
    override var tipo: String? = null
}

data class Num(val num: String, val lin: Int): Exp {
    override var tipo: String? = null
}

data class Var(val nome: String, val lin: Int): Exp {
    var escopo: Bloco? = null
    override var tipo: String? = null
}

fun escopo(no: Any, procs: Map<String, Proc>,
           vars: Map<String, Bloco>, erros: MutableList<String>): Map<String, Bloco> {
    when(no) {
        is Var -> {
            if(vars.containsKey(no.nome)) {
                no.escopo = vars.get(no.nome)
            } else {
                erros.add("variável ${no.nome} não definida na linha ${no.lin}")
            }
        }
        is Soma -> {
            escopo(no.e1, procs, vars, erros)
            escopo(no.e2, procs, vars, erros)
        }
        is Sub -> {
            escopo(no.e1, procs, vars, erros)
            escopo(no.e2, procs, vars, erros)
        }
        is Mult -> {
            escopo(no.e1, procs, vars, erros)
            escopo(no.e2, procs, vars, erros)
        }
        is Div -> {
            escopo(no.e1, procs, vars, erros)
            escopo(no.e2, procs, vars, erros)
        }
        is Menor -> {
            escopo(no.e1, procs, vars, erros)
            escopo(no.e2, procs, vars, erros)
        }
        is Igual -> {
            escopo(no.e1, procs, vars, erros)
            escopo(no.e2, procs, vars, erros)
        }
        is Num -> {}
        is If -> {
            escopo(no.cond, procs, vars, erros)
            escopo(no.th, procs, vars, erros)
            escopo(no.els, procs, vars, erros)
        }
        is Atrib -> {
            if(vars.containsKey(no.lval)) {
                no.escopo = vars.get(no.lval)
            } else {
                erros.add("variável ${no.lval} não definida na linha ${no.lin}")
            }
            escopo(no.rval, procs, vars, erros)
        }
        is Read -> {
            if (vars.containsKey(no.lval)) {
                no.escopo = vars.get(no.lval)
            } else {
                erros.add("variável ${no.lval} não definida na linha ${no.lin}")
            }
        }
        is Chamada -> {
            if (procs.containsKey(no.nome)) {
                no.proc = procs.get(no.nome)
            } else {
                erros.add("chamada a procedimento ${no.nome} não definido na linha ${no.lin}")
            }
        }
        is Write -> escopo(no.exp, procs, vars, erros)
        is Repeat -> {
            val tabcorpo = escopo(no.corpo, procs, vars, erros)
            escopo(no.cond, procs, tabcorpo, erros)
        }
        is Bloco -> {
            val vset = mutableSetOf<String>()
            for(v in no.vars) {
                if(vset.contains(v.nome)) {
                    erros.add("variável $v duplicada na linha ${no.lin}")
                } else vset.add(v.nome)
            }
            val tabbloco =
                    vars.plus(vset.map { v -> Pair(v, no) })
            for(cmd in no.cmds) {
                escopo(cmd, procs, tabbloco, erros)
            }
            return tabbloco
        }
    }
    return vars
}

fun arith(e1: Exp, e2: Exp, procs: Map<String, Proc>,
          vars: Map<String, String>, erros: MutableList<String>, lin: Int):
        String {
    tipo(e1, procs, vars, erros)
    tipo(e2, procs, vars, erros)
    if (e1.tipo == "int" && e2.tipo == "int") {
        return "int"
    } else if (e1.tipo == "int" && e2.tipo == "real") {
        return "real"
    } else if (e1.tipo == "real" && e2.tipo == "int") {
        return "real"
    } else if (e1.tipo == "real" && e2.tipo == "real") {
        return "real"
    } else {
        if(e1.tipo != "int" && e1.tipo != "real")
            erros.add("lado esquerdo de operação aritmética não é número mas ${e1.tipo} na linha $lin")
        if(e2.tipo != "int" && e2.tipo != "real")
            erros.add("lado direito de operação aritmética não é número mas ${e2.tipo} na linha $lin")
        return "int"
    }
}

fun tipo(no: Any, procs: Map<String, Proc>,
           vars: Map<String, String>, erros: MutableList<String>):
        Map<String, String> {
    when (no) {
        is Soma -> no.tipo = arith(no.e1, no.e2, procs, vars, erros, no.lin)
        is Sub -> no.tipo = arith(no.e1, no.e2, procs, vars, erros, no.lin)
        is Mult -> no.tipo = arith(no.e1, no.e2, procs, vars, erros, no.lin)
        is Div -> no.tipo =  arith(no.e1, no.e2, procs, vars, erros, no.lin)
        is Menor -> {
            tipo(no.e1, procs, vars, erros)
            tipo(no.e2, procs, vars, erros)
            if(no.e1.tipo != "int" && no.e1.tipo != "real")
                erros.add("lado esquerdo de comparação não é número mas ${no.e1.tipo} na linha ${no.lin}")
            if(no.e2.tipo != "int" && no.e2.tipo != "real")
                erros.add("lado direito de comparação não é número mas ${no.e2.tipo} na linha ${no.lin}")
            no.tipo = "bool"
        }
        is Igual -> {
            tipo(no.e1, procs, vars, erros)
            tipo(no.e2, procs, vars, erros)
            val te1: String = no.e1.tipo!!
            val te2: String = no.e2.tipo!!
            if(te1 != te2 &&
                    (te1 != "int" || te2 != "real") &&
                    (te1 != "real" || te2 != "int")) {
                erros.add("tipos incompatíveis na igualdade da linha ${no.lin}, lado esquerdo é $te1 e lado direito é $te2")
            }
            no.tipo = "bool"
        }
        is Num -> no.tipo = "int"
        is Var -> no.tipo = vars.get(no.nome)
    }
    return vars
}