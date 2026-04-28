# Utilization Reporting API Documentation

## Overview

This document provides comprehensive API documentation for the Utilization Reporting endpoint. It includes request/response formats, validation rules, error handling, and TypeScript type definitions for frontend integration.

## Endpoint Information

- **URL:** `/api/utilization/report`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Authentication:** Required (Bearer Token)

---

## Request Body Format

### Base Request Structure

```typescript
interface UtilizationReportRequest {
  // Required Fields
  startDate: string;           // Format: "YYYY-MM-DD"
  endDate: string;             // Format: "YYYY-MM-DD"
  reportType: string;          // "RESOURCE" | "PROJECT" | "CLIENT" | "ROLE" | "SUMMARY"
  groupBy: string;             // "WEEKLY" (currently supported)

  // Optional Boolean Fields
  approvedOnly?: boolean;      // Default: true
  includeTrends?: boolean;     // Default: false
  includeAlerts?: boolean;     // Default: false

  // Optional Threshold Fields
  overUtilizationThreshold?: number;  // Range: 1-200, Default: 90
  underUtilizationThreshold?: number; // Range: 1-200, Default: 60

  // Optional Filter Arrays
  resourceIds?: number[];      // Array of resource IDs
  projectIds?: number[];       // Array of project IDs
  roles?: string[];            // Array of role names
  clients?: string[];          // Array of client names
}
```

### Request Examples

#### 1. Minimal Request (Required Fields Only)

```json
{
  "startDate": "2025-05-01",
  "endDate": "2026-03-31",
  "reportType": "SUMMARY",
  "groupBy": "WEEKLY"
}
```

#### 2. Complete Request with All Options

```json
{
  "startDate": "2025-05-01",
  "endDate": "2026-03-31",
  "reportType": "SUMMARY",
  "groupBy": "WEEKLY",
  "approvedOnly": true,
  "includeTrends": true,
  "includeAlerts": true,
  "overUtilizationThreshold": 85,
  "underUtilizationThreshold": 40,
  "resourceIds": [17, 23, 45],
  "projectIds": [101, 102],
  "roles": ["Developer", "Designer"],
  "clients": ["Client A", "Client B"]
}
```

#### 3. Resource-Specific Report

```json
{
  "startDate": "2025-01-01",
  "endDate": "2025-12-31",
  "reportType": "RESOURCE",
  "groupBy": "WEEKLY",
  "resourceIds": [17],
  "includeAlerts": true,
  "overUtilizationThreshold": 90,
  "underUtilizationThreshold": 60
}
```

---

## Response Body Format

### Base Response Structure

```typescript
interface UtilizationReportResponse {
  // Report Metadata
  startDate: string;
  endDate: string;
  reportType: string;
  groupBy: string;
  approvedDataOnly: boolean;

  // Summary Metrics
  totalHours: number;
  plannedHours: number;
  utilizationPercentage: number;
  billableHours: number;
  nonBillableHours: number;
  internalHours: number;

  // Resource Utilization (if reportType includes RESOURCE)
  resourceUtilizations?: ResourceUtilization[];

  // Project Utilization (if reportType includes PROJECT)
  projectUtilizations?: ProjectUtilization[];

  // Client Utilization (if reportType includes CLIENT)
  clientUtilizations?: ClientUtilization[];

  // Role Utilization (if reportType includes ROLE)
  roleUtilizations?: RoleUtilization[];

  // Trends (if includeTrends: true)
  trends?: Record<string, PortfolioTrend[]>;

  // Alerts and Patterns (if includeAlerts: true)
  alerts?: UtilizationAlert[];
  patterns?: UtilizationPattern[];

  // Metadata
  totalResources?: number;
  totalProjects?: number;
  totalClients?: number;
  totalRoles?: number;
  confidenceScore?: number;
}
```

### Supporting Interfaces

```typescript
interface ResourceUtilization {
  resourceId: number;
  resourceName: string;
  role: string;
  totalHours: number;
  billableHours: number;
  nonBillableHours: number;
  internalHours: number;
  plannedHours: number;
  utilizationPercentage: number;
  billableRatio: number;
  utilizationBand: "HIGH" | "OPTIMAL" | "LOW" | "CRITICAL";
  trendSignal: "UP" | "DOWN" | "STABLE";
  alerts: string[];
  consistentlyOverUtilized: boolean;
  consistentlyUnderUtilized: boolean;
  consecutiveWeeksOverThreshold: number;
  consecutiveWeeksUnderThreshold: number;
  confidenceScore: number;
  daysWithApprovedTimesheets: number;
  totalWorkingDays: number;
}

interface ProjectUtilization {
  projectId: number;
  projectName: string;
  clientName: string;
  totalHours: number;
  billableHours: number;
  nonBillableHours: number;
  internalHours: number;
  plannedHours: number;
  utilizationPercentage: number;
  billableRatio: number;
  utilizationBand: string;
  trendSignal: string;
  resourceCount: number;
  averageResourceUtilization: number;
  confidenceScore: number;
}

interface ClientUtilization {
  clientName: string;
  totalHours: number;
  billableHours: number;
  nonBillableHours: number;
  plannedHours: number;
  utilizationPercentage: number;
  billableRatio: number;
  utilizationBand: string;
  projectCount: number;
  resourceCount: number;
  confidenceScore: number;
}

interface RoleUtilization {
  roleName: string;
  totalHours: number;
  billableHours: number;
  nonBillableHours: number;
  plannedHours: number;
  utilizationPercentage: number;
  billableRatio: number;
  utilizationBand: string;
  resourceCount: number;
  averageResourceUtilization: number;
  confidenceScore: number;
}

interface UtilizationAlert {
  id: string;
  type: string;
  severity: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
  scope: string;
  title: string;
  message: string;
  recommendation: string;
  resourceId?: number;
  resourceName?: string;
  currentValue: number;
  thresholdValue: number;
  consecutiveWeeks?: number;
  status: string;
  createdDate: string;
}

interface UtilizationPattern {
  id: string;
  patternType: string;
  severity: string;
  scope: string;
  title: string;
  description: string;
  impact: string;
  recommendation: string;
  resourceId?: number;
  resourceName?: string;
  averageUtilization: number;
  durationWeeks: number;
  weeksOverThreshold?: number;
  weeksUnderThreshold?: number;
  overThreshold?: number;
  underThreshold?: number;
  status: string;
  detectedDate: string;
  lastUpdatedDate: string;
}

interface PortfolioTrend {
  period: string;
  utilization: number;
  billableRatio: number;
  totalHours: number;
  plannedHours: number;
}
```

### Response Examples

#### 1. Summary Report Response

```json
{
  "startDate": "2025-05-01",
  "endDate": "2026-03-31",
  "reportType": "SUMMARY",
  "groupBy": "WEEKLY",
  "approvedDataOnly": true,
  "totalHours": 1760.5,
  "plannedHours": 2080.0,
  "utilizationPercentage": 84.64,
  "billableHours": 1408.4,
  "nonBillableHours": 352.1,
  "internalHours": 156.8,
  "resourceUtilizations": [
    {
      "resourceId": 17,
      "resourceName": "John Doe",
      "role": "Developer",
      "totalHours": 1760.5,
      "billableHours": 1408.4,
      "nonBillableHours": 352.1,
      "internalHours": 156.8,
      "plannedHours": 2080.0,
      "utilizationPercentage": 84.64,
      "billableRatio": 80.0,
      "utilizationBand": "OPTIMAL",
      "trendSignal": "STABLE",
      "alerts": [],
      "consistentlyOverUtilized": false,
      "consistentlyUnderUtilized": false,
      "consecutiveWeeksOverThreshold": 0,
      "consecutiveWeeksUnderThreshold": 0,
      "confidenceScore": 95,
      "daysWithApprovedTimesheets": 240,
      "totalWorkingDays": 260
    }
  ],
  "totalResources": 1,
  "confidenceScore": 95.0
}
```

#### 2. Response with Alerts and Patterns

```json
{
  "startDate": "2025-05-01",
  "endDate": "2026-03-31",
  "reportType": "RESOURCE",
  "groupBy": "WEEKLY",
  "approvedDataOnly": true,
  "totalHours": 1760.5,
  "plannedHours": 2080.0,
  "utilizationPercentage": 84.64,
  "billableHours": 1408.4,
  "nonBillableHours": 352.1,
  "internalHours": 156.8,
  "resourceUtilizations": [...],
  "alerts": [
    {
      "id": "resource-critical-low-17",
      "type": "UNDER_UTILIZATION",
      "severity": "CRITICAL",
      "scope": "RESOURCE",
      "title": "Critical Under-Utilization",
      "message": "John Doe has critical under-utilization at 45.2%",
      "recommendation": "Review workload allocation and project assignments",
      "resourceId": 17,
      "resourceName": "John Doe",
      "currentValue": 45.2,
      "thresholdValue": 60.0,
      "status": "OPEN",
      "createdDate": "2026-04-23"
    }
  ],
  "patterns": [
    {
      "id": "pattern-sustained-low-17",
      "patternType": "SUSTAINED_LOW",
      "severity": "MEDIUM",
      "scope": "RESOURCE",
      "title": "Sustained Low Utilization Pattern",
      "description": "John Doe has maintained utilization below 60.0% for 6 consecutive weeks",
      "impact": "Reduced revenue, inefficient resource allocation",
      "recommendation": "Review project pipeline and capacity utilization",
      "resourceId": 17,
      "resourceName": "John Doe",
      "averageUtilization": 45.2,
      "durationWeeks": 6,
      "weeksUnderThreshold": 6,
      "underThreshold": 60.0,
      "status": "ACTIVE",
      "detectedDate": "2026-04-23",
      "lastUpdatedDate": "2026-04-23"
    }
  ],
  "totalResources": 1,
  "confidenceScore": 95.0
}
```

---

## Validation Rules

### Required Fields
- `startDate`: Must be valid date in "YYYY-MM-DD" format
- `endDate`: Must be valid date in "YYYY-MM-DD" format
- `reportType`: Must be one of: "RESOURCE", "PROJECT", "CLIENT", "ROLE", "SUMMARY"
- `groupBy`: Currently only "WEEKLY" is supported

### Optional Field Constraints
- `overUtilizationThreshold`: Number between 1 and 200
- `underUtilizationThreshold`: Number between 1 and 200
- `approvedOnly`: Boolean (default: true)
- `includeTrends`: Boolean (default: false)
- `includeAlerts`: Boolean (default: false)

### Business Rules
- `startDate` must be before or equal to `endDate`
- Date range cannot exceed 365 days
- `underUtilizationThreshold` should be less than `overUtilizationThreshold`

---

## Error Responses

### Validation Errors (400 Bad Request)

```json
{
  "timestamp": "2026-04-23T06:31:53.386+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "startDate",
      "message": "Start date is required"
    },
    {
      "field": "reportType",
      "message": "Report type is required"
    },
    {
      "field": "overUtilizationThreshold",
      "message": "Over utilization threshold must be at least 1"
    }
  ]
}
```

### Business Logic Errors (400 Bad Request)

```json
{
  "timestamp": "2026-04-23T06:31:53.386+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Start date cannot be after end date"
}
```

```json
{
  "timestamp": "2026-04-23T06:31:53.386+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Report period cannot exceed 365 days"
}
```

### Not Found (404)

```json
{
  "timestamp": "2026-04-23T06:31:53.386+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "No message available",
  "path": "/api/utilization/report"
}
```

### Unauthorized (401)

```json
{
  "timestamp": "2026-04-23T06:31:53.386+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

---

## Frontend Integration Examples

### JavaScript/TypeScript Example

```typescript
// TypeScript Types
interface UtilizationReportRequest {
  startDate: string;
  endDate: string;
  reportType: "RESOURCE" | "PROJECT" | "CLIENT" | "ROLE" | "SUMMARY";
  groupBy: "WEEKLY";
  approvedOnly?: boolean;
  includeTrends?: boolean;
  includeAlerts?: boolean;
  overUtilizationThreshold?: number;
  underUtilizationThreshold?: number;
  resourceIds?: number[];
  projectIds?: number[];
  roles?: string[];
  clients?: string[];
}

// API Service Function
class UtilizationReportingService {
  private baseUrl = '/api/utilization';

  async generateReport(request: UtilizationReportRequest): Promise<any> {
    try {
      const response = await fetch(`${this.baseUrl}/report`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Failed to generate report');
      }

      return await response.json();
    } catch (error) {
      console.error('Error generating utilization report:', error);
      throw error;
    }
  }

  private getAuthToken(): string {
    // Implement your auth token retrieval logic
    return localStorage.getItem('authToken') || '';
  }
}

// Usage Example
const utilizationService = new UtilizationReportingService();

const request: UtilizationReportRequest = {
  startDate: '2025-05-01',
  endDate: '2026-03-31',
  reportType: 'SUMMARY',
  groupBy: 'WEEKLY',
  includeAlerts: true,
  includeTrends: true,
  resourceIds: [17],
  overUtilizationThreshold: 90,
  underUtilizationThreshold: 60
};

utilizationService.generateReport(request)
  .then(response => {
    console.log('Utilization Report:', response);
    // Process the response data
  })
  .catch(error => {
    console.error('Error:', error);
    // Handle error (show user notification, etc.)
  });
```

### React Hook Example

```typescript
import { useState, useCallback } from 'react';

interface UseUtilizationReportReturn {
  data: any | null;
  loading: boolean;
  error: string | null;
  generateReport: (request: UtilizationReportRequest) => Promise<void>;
  reset: () => void;
}

export const useUtilizationReport = (): UseUtilizationReportReturn => {
  const [data, setData] = useState<any | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const generateReport = useCallback(async (request: UtilizationReportRequest) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/utilization/report', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('authToken')}`
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to generate report');
      }

      const result = await response.json();
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error occurred');
    } finally {
      setLoading(false);
    }
  }, []);

  const reset = useCallback(() => {
    setData(null);
    setError(null);
    setLoading(false);
  }, []);

  return { data, loading, error, generateReport, reset };
};
```

---

## Testing

### Sample cURL Commands

```bash
# Basic Request
curl -X POST http://localhost:8080/api/utilization/report \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "startDate": "2025-05-01",
    "endDate": "2026-03-31",
    "reportType": "SUMMARY",
    "groupBy": "WEEKLY"
  }'

# Request with Alerts and Trends
curl -X POST http://localhost:8080/api/utilization/report \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "startDate": "2025-05-01",
    "endDate": "2026-03-31",
    "reportType": "RESOURCE",
    "groupBy": "WEEKLY",
    "resourceIds": [17],
    "includeAlerts": true,
    "includeTrends": true,
    "overUtilizationThreshold": 90,
    "underUtilizationThreshold": 60
  }'
```

---

## Notes and Limitations

### Current Limitations
1. **groupBy**: Only "WEEKLY" is currently supported. "DAILY" and "MONTHLY" are planned but not implemented.
2. **Date Range**: Maximum 365 days between start and end dates.
3. **Performance**: Large date ranges with many resources may result in slower response times.

### Best Practices
1. Always include error handling for API calls
2. Use appropriate date formats ("YYYY-MM-DD")
3. Consider pagination for large datasets
4. Cache responses when appropriate to reduce server load
5. Use the `approvedOnly` filter for production reports to ensure data accuracy

### Future Enhancements
- Support for "DAILY" and "MONTHLY" grouping
- Real-time utilization monitoring
- Advanced filtering options
- Export to additional formats (PDF, Excel)
- Historical trend analysis
- Predictive utilization forecasting
