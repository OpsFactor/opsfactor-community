package com.opsfactor.community.web.configuration.dataformat;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests real Spring converter selection and file output without an application/database. */
class DataFormatHttpIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mvc;

    @BeforeEach
    void configureMvc() {

        mvc = MockMvcBuilders.standaloneSetup(new SampleController(), new TypedIntegrationController())
                .setControllerAdvice(new DataFormatExceptionHandler())
                .setContentNegotiationManager(new ContentNegotiationManager(
                        new DataFormatNegotiationStrategy(), new HeaderContentNegotiationStrategy()))
                .setMessageConverters(new DataFormatHttpMessageConverter(mapper),
                        new MappingJackson2HttpMessageConverter(mapper), new StringHttpMessageConverter(),
                        new ByteArrayHttpMessageConverter())
                .build();
        DataFormatLegacyExportAliases.registerAliases(mvc.getDispatcherServlet().getWebApplicationContext()
                .getBean(RequestMappingHandlerMapping.class));

    }

    @Test
    void missingFormatAndLegacyPathsKeepJsonEvenWhenOldClientAsksForExcel() throws Exception {

        mvc.perform(get("/api/secured/data/sample").accept("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("000123"));
        mvc.perform(get("/api/secured/data/file/sample"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0][0]").value("Human identifier"));
        mvc.perform(get("/api/secured/dataupload/json/sample"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("000123"));

    }

    @Test
    void xlsxIsARealWorkbookAndRetainsTemplateHeadersAndTextIdentifiers() throws Exception {

        byte[] file = mvc.perform(get("/api/secured/data/sample").param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            assertEquals("Human identifier", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("000123", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals(1234.56, workbook.getSheetAt(0).getRow(1).getCell(1).getNumericCellValue(), 0.00001);
        }

    }

    @Test
    void negotiatedDownloadsUseResourceNamesWithoutApiPrefixes() throws Exception {

        for (String format : List.of("xlsx", "csv", "arrow", "parquet")) {
            String disposition = mvc.perform(get("/api/secured/data/sample").param("format", format))
                    .andExpect(status().isOk()).andReturn().getResponse().getHeader("Content-Disposition");
            assertEquals("sample." + format,
                    org.springframework.http.ContentDisposition.parse(disposition).getFilename());
        }
        String legacy = mvc.perform(get("/api/secured/dataupload/json/legacydto").param("format", "arrow"))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("Content-Disposition");
        assertEquals("legacydto.arrow", org.springframework.http.ContentDisposition.parse(legacy).getFilename());
        String report = mvc.perform(get("/api/secured/planning/demand/changelog/17").param("format", "xlsx"))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("Content-Disposition");
        assertEquals("planning-demand-changelog-17.xlsx",
                org.springframework.http.ContentDisposition.parse(report).getFilename());

    }

    @Test
    void csvDefaultsAndLocaleSeparatorsAreAppliedOnTheServer() throws Exception {

        String standard = mvc.perform(get("/api/secured/data/sample").param("format", "csv"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(standard.contains("000123,1234.56"), standard);
        String local = mvc.perform(get("/api/secured/data/sample").param("format", "csv")
                        .param("fieldSeparator", ";").param("decimalSeparator", "comma"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(local.contains("000123;1234,56"), local);
        String commaBoth = mvc.perform(get("/api/secured/data/sample").param("format", "csv")
                        .param("decimalSeparator", "comma"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(commaBoth.contains("000123,\"1234,56\""), commaBoth);

    }

    @Test
    void arrowAndParquetCanBePostedBackAsTypedDtosAndConfirmationsStayJson() throws Exception {

        for (String format : List.of("arrow", "parquet")) {
            String contentType = format.equals("arrow") ? "application/vnd.apache.arrow.stream" : "application/vnd.apache.parquet";
            byte[] file = mvc.perform(get("/api/secured/data/sample").param("format", format))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(contentType))
                    .andReturn().getResponse().getContentAsByteArray();
            mvc.perform(post("/api/secured/data/sample").contentType(contentType).content(file))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("000123")));
        }

    }

    @Test
    void invalidFormatsFailAndNativeBinaryDownloadsAreNotRenegotiated() throws Exception {

        mvc.perform(get("/api/secured/data/sample").param("format", "invalid")).andExpect(status().isNotAcceptable());
        mvc.perform(post("/api/secured/data/sample").contentType("application/x-unknown").content("data"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/secured/data/native"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
        mvc.perform(get("/api/secured/data/failure").param("format", "arrow"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid selection"));

    }

    @Test
    void oldFilePathsBecomeOnlyExplicitFormatAliases() {

        assertEquals("/api/secured/data/profitlossbydfu/{id}",
                DataFormatLegacyExportAliases.canonicalExportPath("/api/secured/dataupload/profitlossbydfu/new/file/{id}"));
        assertEquals("/api/secured/data/sample", DataFormatLegacyExportAliases.canonicalExportPath("/api/secured/data/file/sample"));
        assertNull(DataFormatLegacyExportAliases.canonicalExportPath("/api/secured/dataupload/json/sample"));

    }

    @Test
    void inheritedGenericHandlersResolveEmptyListSchemaAndBinaryIntegrationEnvelope() throws Exception {

        for (String format : List.of("arrow", "parquet")) {
            String contentType = format.equals("arrow") ? "application/vnd.apache.arrow.stream" : "application/vnd.apache.parquet";
            byte[] emptyFile = mvc.perform(get("/api/secured/data/generic").param("format", format).param("empty", "true"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            var rowsType = mapper.getTypeFactory().constructCollectionType(List.class, Sample.class);
            Object rows = new DataFormatCodec(mapper).read(new ByteArrayInputStream(emptyFile), rowsType, DataFormat.fromParameter(format));
            assertEquals(List.of(), rows);
            byte[] file = mvc.perform(get("/api/secured/data/generic").param("format", format))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            mvc.perform(post("/api/secured/data/generic").contentType(contentType).content(file))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("000123")));
        }

    }

    @Test
    void externalReportEnvelopeKeepsJsonAndExportsUsingItsViewColumns() throws Exception {

        mvc.perform(get("/api/secured/deployment/detailedreport/17"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].identifier").value("000123"));
        String csv = mvc.perform(get("/api/secured/deployment/detailedreport/17").param("format", "csv"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("Material code,Output quantity"), csv);
        assertTrue(csv.contains("000123,1234.56"), csv);
        byte[] xlsx = mvc.perform(get("/api/secured/planning/demand/changelog/17").param("format", "xlsx"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertEquals("Material code", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("000123", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            var calculatedCost = workbook.getSheetAt(0).getRow(1).getCell(2);
            assertEquals(CellType.NUMERIC, calculatedCost.getCellType());
            assertEquals(103.51581249999998d, calculatedCost.getNumericCellValue());
        }

    }

    @Test
    void concreteExportsAndOldDtoMethodsGainCanonicalAliasesWithoutChangingExistingUrls() throws Exception {

        mvc.perform(get("/api/secured/data/legacydto"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("000123"));
        mvc.perform(get("/api/secured/data/plan/17").param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        mvc.perform(get("/api/secured/dataupload/plan/new/file/17"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0][0]").value("Human identifier"));
        mvc.perform(get("/api/secured/data/plan/17"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mvc.perform(get("/api/secured/data/sample").param("format", "XLSX")).andExpect(status().isNotAcceptable());

    }

    public static class SampleKey {
        public String id;
    }

    public static class HierarchicalRow {
        public String id;
        public Sample details;
    }

    @Test
    void hierarchicalOutputsFailBeforeDownloadButJsonAndColumnarFormatsKeepTheirStructure() throws Exception {

        for (String format : List.of("xlsx", "csv")) {
            for (String empty : List.of("false", "true")) {
                mvc.perform(get("/api/secured/data/hierarchical").param("format", format).param("empty", empty))
                        .andExpect(status().isNotAcceptable())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(header().doesNotExist("Content-Disposition"))
                        .andExpect(jsonPath("$.code").value("HIERARCHICAL_EXPORT_NOT_SUPPORTED"))
                        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("múltiplos níveis hierárquicos")))
                        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("details")));
            }
        }
        mvc.perform(get("/api/secured/data/hierarchical"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].details.id").value("000123"));
        for (String format : List.of("arrow", "parquet")) {
            byte[] file = mvc.perform(get("/api/secured/data/hierarchical").param("format", format))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            var type = mapper.getTypeFactory().constructCollectionType(List.class, HierarchicalRow.class);
            @SuppressWarnings("unchecked") List<HierarchicalRow> rows = (List<HierarchicalRow>) new DataFormatCodec(mapper)
                    .read(new ByteArrayInputStream(file), type, DataFormat.fromParameter(format));
            assertEquals("000123", rows.getFirst().details.key.id);
        }

    }

    public static class Sample {
        @JsonUnwrapped public SampleKey key;
        public BigDecimal quantity;
    }

    public static class IntegrationDto<R, P, F, O> {
        public List<R> data;
        public O options;
        public F dataFilter;
    }

    public static class AgGridDTO {
        public List<GridColumn> columnDefs;
        public List<Map<String, Object>> data;
    }

    public static class GridColumn {
        public String field;
        public String headerName;
        public List<GridColumn> children;

        GridColumn(String field, String header) {

            this.field = field;
            this.headerName = header;

        }
    }

    /** Mirrors the real inherited controller signature, including its type-parameter positions. */
    public abstract static class IntegrationControllerAbstract<E, R, P, F, S, O> {

        protected abstract List<R> records();

        @GetMapping("/api/secured/data/generic")
        public List<R> getDataRecordDtoList(@RequestParam(value = "empty", defaultValue = "false") boolean empty) {

            return empty ? List.of() : records();

        }

        @PostMapping("/api/secured/data/generic")
        public ResponseEntity<String> saveIntegrationDto(@RequestBody IntegrationDto<R, P, F, O> integrationDto) {

            assertInstanceOf(Sample.class, integrationDto.data.getFirst());
            Sample row = (Sample) integrationDto.data.getFirst();
            assertEquals(new BigDecimal("1234.56"), row.quantity);
            return ResponseEntity.ok(row.key.id);

        }
    }

    @RestController
    public static class TypedIntegrationController extends IntegrationControllerAbstract<Object, Sample, SampleKey, Object, Object, Object> {

        @Override
        protected List<Sample> records() {

            return new SampleController().rows();

        }
    }

    @RestController
    static class SampleController {

        @GetMapping("/api/secured/data/hierarchical")
        public List<HierarchicalRow> hierarchical(@RequestParam(value = "empty", defaultValue = "false") boolean empty) {

            HierarchicalRow row = new HierarchicalRow();
            row.id = "parent";
            row.details = rows().getFirst();
            return empty ? List.of() : List.of(row);

        }

        @GetMapping({"/api/secured/data/sample", "/api/secured/dataupload/json/sample", "/api/secured/dataupload/json/legacydto"})
        public List<Sample> rows() {

            Sample sample = new Sample();
            sample.key = new SampleKey();
            sample.key.id = "000123";
            sample.quantity = new BigDecimal("1234.56");
            return List.of(sample);

        }

        @GetMapping(value = "/api/secured/data/sample", params = "format=xlsx")
        public List<List<Object>> workbook() {

            return sheet();

        }

        @GetMapping(value = "/api/secured/data/sample", params = "format=csv")
        public List<List<Object>> csv() {

            return sheet();

        }

        @GetMapping("/api/secured/dataupload/plan/new/file/{id}")
        public List<List<Object>> getPlanFile(@PathVariable("id") long id) {

            return sheet();

        }

        @GetMapping("/api/secured/data/file/sample")
        public List<List<Object>> sheet() {

            return List.of(List.of("Human identifier", "Quantity"), List.of("000123", new BigDecimal("1234.56")));

        }

        @PostMapping("/api/secured/data/sample")
        public ResponseEntity<String> save(@RequestBody List<Sample> samples) {

            assertEquals(new BigDecimal("1234.56"), samples.getFirst().quantity);
            return ResponseEntity.ok(samples.getFirst().key.id);

        }

        @GetMapping("/api/secured/data/native")
        public ResponseEntity<byte[]> nativeDownload() {

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(new byte[]{1, 2, 3});

        }

        @GetMapping("/api/secured/data/failure")
        public ResponseEntity<Map<String, String>> failedSelection() {

            return ResponseEntity.badRequest().body(Map.of("message", "Invalid selection"));

        }

        @GetMapping({"/api/secured/deployment/detailedreport/{id}", "/api/secured/planning/demand/changelog/{id}"})
        public ResponseEntity<AgGridDTO> gridReport(@PathVariable("id") long id) {

            AgGridDTO report = new AgGridDTO();
            report.columnDefs = List.of(new GridColumn("identifier", "Material code"),
                    new GridColumn("quantity", "Output quantity"), new GridColumn("calculatedCost", "Calculated cost"));
            report.data = List.of(Map.of("identifier", "000123", "quantity", new BigDecimal("1234.56"),
                    "calculatedCost", 103.51581249999998d));
            return ResponseEntity.ok(report);

        }
    }
}
