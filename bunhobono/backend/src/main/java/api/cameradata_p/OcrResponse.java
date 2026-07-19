package api.cameradata_p;

public record OcrResponse(
        boolean saved,
        boolean registered,
        boolean gateOpened,
        Integer gateNo
) {
}
