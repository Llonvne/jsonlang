package cn.llonvne.jsonlang.core.codegen

/**
 * 后端注册
 * 初始化所有可用的后端
 */
object BackendRegistration {
    
    private var initialized = false
    
    /**
     * 初始化后端注册表
     */
    fun initialize() {
        if (initialized) return
        
        // 注册 Kotlin 后端
        BackendRegistry.register("kotlin") { KotlinBackend() }
        
        // 注册 Go 后端
        BackendRegistry.register("go") { GoBackend() }
        
        initialized = true
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized
}