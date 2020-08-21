package knf.kuma.widgets

import org.jetbrains.anko.doAsync

object Work{

    fun doWork(callback: (Any) -> Unit){
        doAsync {
            val result: Any = Object()
            //Some heavy work
            callback(result)
        }
    }

    fun doWork(callback: Inter){
        doAsync {
            val result: Any = Object()
            //Some heavy work
            callback.func1(result)
            callback.func2(result)
        }
    }

    fun doWork(callback: Abs){
        doAsync {
            val result: Any = Object()
            //Some heavy work
            callback.func1(result)
            callback.func2(result)
            callback.func3(result)
            callback.func4(result)
        }
    }

}

class Main{

    fun main(){
        //Lamda
        Work.doWork {

        }
        //Interfaz
        Work.doWork(object : Inter{
            override fun func1(any: Any) {
                //Obligatorio implementar
            }

            override fun func2(any: Any) {
                //Obligatorio implementar
            }
        })
        //Abstracto
        Work.doWork(object : Abs(){
            override fun func1(any: Any) {
                //Opcional implementar
            }

            override fun func4(any: Any) {
                //Opcional implementar
            }
        })
    }

}

interface Inter{
    fun func1(any: Any)
    fun func2(any: Any)
}

abstract class Abs{
    open fun func1(any: Any){}
    open fun func2(any: Any){}
    open fun func3(any: Any){}
    open fun func4(any: Any){}
}
