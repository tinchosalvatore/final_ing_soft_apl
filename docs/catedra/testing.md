# Guía: testing (unitarios, integración, arquitectura y E2E con Playwright)

Responsable: agente **testing** (escribe los tests de los criterios) y agente **seguridad** como verificador (controla que representen la spec). El agente que implementa no modifica los tests del agente testing.

## 1. Qué se aplica

| Tipo | Herramienta | Qué cubre en este proyecto | Quién lo escribe |
|---|---|---|---|
| **Unitario** | JUnit 5 + AssertJ + Mockito | Reglas de negocio puras: validaciones de gasto (001) y **todo el cálculo del resumen (002)** | Tester, **antes** del código (TDD) |
| **Integración** | Spring Boot Test + **Testcontainers (Postgres)** | Endpoints reales contra una base real: control de acceso (001-CA04 a CA11), filtros, categoría en uso, logs (004) | JHipster genera los de CRUD; el tester agrega los de los criterios |
| **Arquitectura** | **ArchUnit** | Capas Resource → Service → Repository (lo genera JHipster: `TechnicalStructureTest`). Se agrega: los `Resource` no usan `Repository` directo y `web.rest` no depende de `domain` en las firmas públicas de los endpoints nuevos | Tester |
| **Frontend unitario** | **Vitest** | Lo que genera JHipster + la lógica del módulo offline (cola de pendientes, estados pendiente/rechazado) | Tester |
| **E2E** | **Playwright**, grabado con **codegen** | Los criterios marcados "Flujo E2E" y todos los de la spec 003 | Tester |

### TDD con agentes (red → green → refactor)
1. **Rojo:** el tester, en una sesión nueva y leyendo solo la spec, escribe los tests. Se corren y **tienen que fallar** (si pasan sin código, el test está mal).
2. **Verde:** el implementador escribe el mínimo código para que pasen.
3. **Refactor:** el implementador mejora el código sin cambiar el comportamiento; los tests siguen verdes.

Se aplica completo en la **spec 002** (lógica pura, ideal para TDD) y en los criterios de control de acceso de la 001. Para el resto, el tester escribe los tests en paralelo o antes, pero siempre a partir de la spec.

## 2. Cómo lo tiene que hacer el agente

### Trazabilidad
- Cada test nombra el criterio que cubre: en Java `@DisplayName("002-CA04 variación positiva")`; en Playwright `test('003-CA04 carga sin conexión', ...)`.
- **Si para escribir un test hay que decidir algo que la spec no dice, el tester frena y lo reporta**: la spec está incompleta. No inventa.

### Dónde va cada test
| Tipo | Ubicación |
|---|---|
| Unitarios backend | `src/test/java/com/trackergastos/service/...Test.java` |
| Integración backend | `src/test/java/com/trackergastos/web/rest/...IT.java` (convención de JHipster) |
| ArchUnit | `src/test/java/com/trackergastos/TechnicalStructureTest.java` (compartido) o una clase nueva `ArquitecturaTest.java` |
| Vitest | junto al archivo, `*.spec.ts` |
| E2E de negocio | `src/test/javascript/playwright/e2e/negocio/NNN-*.pw.ts` |

### E2E con Playwright + codegen
1. Levantar la app (y para la spec 003, el build de producción).
2. `npx playwright codegen http://localhost:8080` y recorrer el flujo del criterio.
3. Pegar el código grabado en el `.pw.ts` correspondiente (extensión **`.pw.ts`**, si no Vitest lo toma).
4. Ajustarlo:
   - **Locators por rol, label o texto** (`getByRole('button', { name: 'Guardar' })`, `getByLabel('Monto')`). Nada de selectores CSS ni XPath. Si un elemento no tiene rol o label accesible, se le agrega un `data-testid` (y se avisa al implementador).
   - Una **aserción explícita** por cada resultado esperado del criterio (`expect(...)`).
   - Login con los helpers que genera JHipster en `playwright/support/`, no regrabado en cada test.
   - Offline: `await context.setOffline(true)`.
   - Datos de prueba creados por el propio test (por API) y borrados al final. **Nunca datos reales.**
5. La "grabación reproducible" es el **trace**: los tests de negocio usan `test.use({ trace: 'on' })`. Se ve con `npx playwright show-trace`.
6. **Page Object Model**: opcional. Si un flujo se repite en 3 o más tests (por ejemplo, "cargar gasto"), se extrae a `playwright/pages/GastoPage.ts`.

### Datos y usuarios de prueba
- Los tests de integración crean sus usuarios (`ana`, `beto`) y sus datos.
- Los E2E usan el usuario `user` de JHipster y crean un segundo usuario por API para los casos de aislamiento.

## 3. Cómo se verifica
- `./mvnw -ntp verify` y `npm test` en verde; E2E en verde en el CI.
- `verify.md`: cada criterio de la spec tiene al menos un test con su ID, y el verificador confirma que el test **realmente** comprueba el resultado esperado (no solo que "no explota").

## 4. Qué queda afuera y por qué
| Concepto | Motivo |
|---|---|
| TestNG | JHipster usa JUnit 5; mezclar frameworks no aporta |
| Selenium, Cypress | Se eligió Playwright (enunciado, ADR-0003) |
| Pruebas de rendimiento (carga, estrés, soak, picos) | Fuera del alcance de tiempo |
| Screenplay pattern | Page Object alcanza para este tamaño |
| Umbral de cobertura | Se prioriza que cada criterio tenga su test, no un porcentaje |
| Pruebas exploratorias | Las hace el humano a mano antes de la entrega; no se automatizan |

## 5. Referencias
ADR-0003, specs 001 a 004 (tablas de criterios), `agentes-sdd.md` §2.
