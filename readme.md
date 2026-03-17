Especificación Lógica: Motor de Renderizado Espacial Nativo Basado en Datos (V5 - Android)
1. Resumen Ejecutivo
Este documento define la arquitectura de un motor de renderizado agnóstico nativo para Android. El sistema utiliza el árbol de Vistas del sistema operativo (o Jetpack Compose) como un lienzo dinámico. Resuelve el desacoplamiento total entre diseño, funcionalidades de hardware y lógica de negocio mediante una interfaz dirigida por datos (JSON), permitiendo la construcción procedimental de aplicaciones completas controladas remotamente con latencia cero.

2. Definición de Tipos de Datos (Interfaces)
Para que el motor sea replicable y universal, el "Árbol de Definición" (JSON) debe deserializarse estáticamente bajo este contrato de estructura:

Interfaz Nodo:

Identidad (ID): Cadena de texto única.
Ruta (Path): Cadena jerárquica (ej. app.pantalla.contenedor.boton).
Tipo (Type): Identificador del componente nativo a instanciar (ej. CONTAINER, TEXT, IMAGE, INPUT, MEDIA_PICKER, MAP).
Regla de Instancias: Cada instancia generada procedimentalmente en una lista debe heredar el Path del padre más un sufijo de índice único (ej. lista.item:ins[4]).
Propiedades_Estéticas: Mapa de pares [Clave: Valor] para el estilo visual (ej. backgroundColor, padding, cornerRadius).
Directivas_Lógicas: Mapa de pares [Clave: Instrucción] para definir el comportamiento o configuración de hardware (ej. allowVideo, maxDuration).
Hijos: Lista ordenada de objetos tipo Nodo.
Cámara_Memoria: Mapa volátil interno para preservar estados locales del usuario (ej. texto ingresado, archivo seleccionado) antes de su transmisión.
Constraints_Base: Matriz de reglas de posicionamiento nativo (dimensiones, márgenes, pesos).
3. Gestión del Ciclo de Vida (Lifecycle)
El motor gestiona el estado de los nodos acoplándose estrictamente al ciclo de vida nativo del sistema (Activity/Fragment) para prevenir Memory Leaks:

Fase de Montaje (Mount): El motor lee el dato, resuelve el Type y crea la entidad física nativa correspondiente. Si la Directiva_Lógica requiere permisos del sistema operativo (Cámara, GPS, Micrófono), el nodo gestiona la solicitud asíncrona en esta fase.
Límite de Profundidad: El proceso de construcción debe abortar si la jerarquía excede los 32 niveles de profundidad para prevenir el colapso de la pila de llamadas (Stack Overflow).
Fase de Actualización (Update): Al recibir un parche de datos remotos, se ejecuta una Fusión Profunda. Si una propiedad entra en conflicto, se aplica la Matriz de Prioridad: Interacción de Usuario > Parche Remoto > Estado Base. Solo se re-renderizan las propiedades modificadas (Diffing).
Fase de Desmontaje (Unmount): El motor debe ejecutar un Contrato de Limpieza estricto:
Cancelar todos los trabajos asíncronos (Coroutines/Threads) vinculados a la identidad del nodo.
Liberar recursos de hardware en uso (MediaRecorder, CameraDevice, LocationManager).
Destruir cachés de memoria pesada (Bitmaps).
Desvincular escuchadores globales y vaciar la Cámara_Memoria.
4. El Bus de Telemetría y Normalización de Inputs
El sistema no procesa lógica de negocio localmente. Todo evento de usuario se normaliza y se emite hacia un controlador externo (Servidor/WebSocket).

Normalización de Inputs (Input Mapping): Gestos y acciones del usuario (Click, LongPress, Swipe, InputText) se capturan, se empaquetan junto con el ID del nodo y su Cámara_Memoria, y se despachan al Bus de Telemetría.
Delegación Asíncrona: Operaciones pesadas dictadas por las Directivas_Lógicas (como compresión de imágenes o videos antes de enviar) deben ejecutarse fuera del hilo principal (Background Thread).
5. Sistema de Coordenadas, Unidades y Posicionamiento
Unidades: Toda dimensión física se interpreta como dp (Density-independent Pixels). Toda dimensión tipográfica como sp (Scale-independent Pixels).
Contexto de Apilamiento: El orden de pintado se rige por la posición de los Hijos en la lista del JSON (el último hijo se dibuja encima).
Z-Index Lógico: Elevaciones o sombras se inyectan utilizando la propiedad de elevación nativa del sistema, calculada por la GPU (Hardware Acceleration).
6. Diccionario de Tipos Base y Comportamientos Universales
El motor debe ser capaz de instanciar y gestionar el ciclo de vida de los siguientes Types universales, configurados por sus Directivas_Lógicas:

Renderizado de Listas (RECYCLER_VIEW): Instanciación procedimental optimizada. Solo mantiene en memoria las vistas visibles en la pantalla (Culling Lógico).
Captura de Medios (MEDIA_CAPTURE): Invoca intents nativos de Cámara o Galería. Guarda la URI resultante en la Cámara_Memoria. Si la directiva lo dicta, lanza módulos nativos de edición (recorte/duración).
Grabación Continua (AUDIO_RECORD): Implementa una máquina de estados local (IDLE -> RECORDING -> PAUSED) controlada por gestos. Guarda el archivo temporal en la Cámara_Memoria.
Visor de Mapas (MAP_VIEW): Instancia un lienzo geográfico interactivo con geocodificación inversa (coordenadas a texto) basada en la posición central.
Acciones (ACTION_EMITTER): Cualquier nodo con directiva de acción despacha su Cámara_Memoria a través del Bus de Telemetría hacia la ruta (Path) especificada.
7. Algoritmos Core
A. Gestión de la Cámara de Memoria (Restauración y Transmisión)

INICIO GuardarEstadoLocal(Nodo, Propiedad, Valor)
    SI Nodo.Cámara_Memoria NO TIENE Propiedad ENTONCES
        Nodo.Cámara_Memoria[Propiedad] = Valor
    FIN SI
FIN

INICIO TransmitirEstado(Nodo, Evento)
    Payload = {
        "origen": Nodo.Path,
        "evento": Evento,
        "memoria": Nodo.Cámara_Memoria
    }
    Emitir_A_Bus_Telemetria(Payload)
    // Opcional según directiva: LIMPIAR Cámara_Memoria
FIN
B. Algoritmo de Fusión de Datos (Patching)

INICIO Fusionar(Principal, Parcial, Nivel_Actual)
    SI Nivel_Actual > 32 ENTONCES ERROR "Profundidad Excedida"
    
    PARA cada Llave en Parcial:
        SI es Lista ENTONCES Sobrescribir Principal[Llave]
        SI es Objeto ENTONCES 
            SI existe en Principal ENTONCES 
                Fusionar(Principal[Llave], Parcial[Llave], Nivel_Actual + 1)
            SI NO ENTONCES Principal[Llave] = Parcial[Llave]
        SI NO ENTONCES Principal[Llave] = Parcial[Llave]
FIN
8. Casos de Uso y Excepciones
Desincronización de Referencias: Si un nodo recibe una directiva lógica para interactuar con el estado de un nodo "Hermano" que ha sido desmontado, el Bus de Telemetría purga la instrucción de forma silenciosa.
Validación de Fallback (Type Desconocido): Si el JSON remoto inyecta un Type que el motor nativo (en su versión actual) no comprende, la Fábrica de Nodos instanciará un "Cubo Espacial Vacío" (Contenedor transparente de 0x0 dp). Esto garantiza que el árbol de renderizado jamás sufra un colapso fatal (Crash).
Resiliencia de Hardware: Si un usuario deniega permanentemente un permiso de hardware crítico dictado por la Directiva_Lógica (ej. Micrófono), el nodo despacha una señal de error al Bus de Telemetría y se auto-suspende visualmente.
