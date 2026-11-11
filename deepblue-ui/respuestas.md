# Respuestas — Laboratorio DeepBlue Rescue (Frontend React)

## "Para pensar"

**Checkpoint 1.** La URL es `/api/...` (relativa) porque el navegador la envía al mismo origen que sirve la página (`localhost:5173`) y el proxy de Vite la reenvía a Spring Boot (`localhost:8080`). Así no hay llamada entre orígenes distintos, no se activa CORS y no hay que modificar el backend. Además, en producción la URL base se puede cambiar sin tocar el código.

**Checkpoint 2.** TanStack Query decide `isError` según si la promesa de `queryFn`/`mutationFn` se rechaza. Si `request` *devolviera* el error como valor, la librería lo trataría como un dato exitoso (`isError=false`, `data` = el error), no reintentaría ni activaría `onError`, y cada componente tendría que revisar manualmente si lo recibido es un error. Al lanzar `ApiError`, el manejo queda centralizado y `error.status/message/details` llegan listos a `ErrorMessage`.

**Checkpoint 3.** Sin `status` en la `queryKey` (`['rescue-cases']`), todos los filtros compartirían una sola entrada de caché: al cambiar de estado se seguiría mostrando la lista del estado anterior, porque la clave no cambió y no se dispararía una consulta nueva. Con `status` en la clave, cada estado tiene su caché y el cambio de filtro lanza la consulta automáticamente.

**Checkpoint 4.** `enabled` evita ejecutar la consulta hasta que la condición sea verdadera. Sin él, con `animalCode` `undefined` se llamaría a `/api/animals/undefined`, el backend respondería 404 y se mostraría un error falso (más llamadas inútiles al servidor).

**Checkpoint 5.** `invalidateQueries({ queryKey: ['rescue-cases'] })` marca como obsoletas todas las consultas cuya clave *empiece* por `['rescue-cases']` (coincidencia por prefijo) y refresca las que están en pantalla. Afecta a todas las listas por estado (`['rescue-cases','ADMITTED']`, `['rescue-cases','UNDER_EVALUATION']`, …) porque todas comparten ese prefijo; así el caso desaparece de la lista de origen y aparece en la de destino sin recargar la página.

**Checkpoint 6.** `useReducer` agrupa los cuatro campos en un único estado con una sola función que define las transiciones (`field`, `reset`): el reinicio es una sola acción y la lógica queda en un lugar, en vez de cuatro `useState` y cuatro `set...` que reiniciar. `invalidateQueries` usa `['animal', animalCode]` sin sufijos porque el prefijo cubre las tres consultas del animal (`animal`, `treatments`, `eligibility`); así se refrescan tratamientos y elegibilidad a la vez, y si se añade otra consulta del animal también queda cubierta.

## Pruebas del Paso 5 (anotar lo observado en el navegador)

| Prueba | Esperado | Observado |
|---|---|---|
| ADMITTED → UNDER_EVALUATION | 200, el caso pasa de lista sin recargar | |
| ADMITTED → RELEASED | 409 con mensaje de la regla de negocio, en rojo | |

## Pruebas del Paso 6 (anotar lo observado en el navegador)

| Prueba | Esperado | Observado |
|---|---|---|
| Descripción < 10 caracteres | 400, `description` visible | |
| specialistCode vacío | 400, `specialistCode` visible | |
| Fecha futura | 400, "Treatment date cannot be in the future" | |
| Fecha vacía | 400 (mensaje según el handler) | |
| specialistCode = SP-999 | 404 | |
| Datos válidos | 201, aparece en la tabla y el formulario se limpia | |
