package knf.kuma.download

class UriValidation {
    var errorMessage: String? = null
    var isValid = false

    override fun toString(): String {
        return errorMessage ?: "Error desconocido"
    }
}