import React, { useState, useEffect } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card'
import { Button } from '../ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select'
import { Badge } from '../ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs'
import { Textarea } from '../ui/textarea'
import { Progress } from '../ui/progress'
import { 
  RefreshCw, 
  Download, 
  Calendar, 
  GitBranch, 
  MessageSquare, 
  FileText,
  TrendingUp,
  Clock
} from 'lucide-react'
import { WorkSummary as WorkSummaryType, TimeFilter } from '../../types'
import { apiClient } from '../../utils/api'
import { toast } from 'sonner'

const WorkSummary: React.FC = () => {
  const { user } = useAuth()
  const [summaries, setSummaries] = useState<WorkSummaryType[]>([])
  const [selectedSummary, setSelectedSummary] = useState<WorkSummaryType | null>(null)
  const [loading, setLoading] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [selectedTimeFilter, setSelectedTimeFilter] = useState('week')

  const timeFilters: TimeFilter[] = [
    {
      label: 'Today',
      value: 'today',
      startDate: new Date(new Date().setHours(0, 0, 0, 0)),
      endDate: new Date(new Date().setHours(23, 59, 59, 999))
    },
    {
      label: 'This Week',
      value: 'week',
      startDate: new Date(new Date().setDate(new Date().getDate() - 7)),
      endDate: new Date()
    },
    {
      label: 'Last 2 Weeks',
      value: '2weeks',
      startDate: new Date(new Date().setDate(new Date().getDate() - 14)),
      endDate: new Date()
    },
    {
      label: 'Last Month',
      value: 'month',
      startDate: new Date(new Date().setMonth(new Date().getMonth() - 1)),
      endDate: new Date()
    },
    {
      label: 'Last Year',
      value: 'year',
      startDate: new Date(new Date().setFullYear(new Date().getFullYear() - 1)),
      endDate: new Date()
    }
  ]

  useEffect(() => {
    fetchSummaries()
  }, [selectedTimeFilter])

  const fetchSummaries = async () => {
    try {
      setLoading(true)
      const filter = timeFilters.find(f => f.value === selectedTimeFilter)
      if (!filter) return

      const response = await apiClient.get<{ summaries: WorkSummaryType[] }>(
        `/api/work-summaries/my-summaries?startDate=${filter.startDate.toISOString()}&endDate=${filter.endDate.toISOString()}`
      )
      
      setSummaries(response.summaries || [])
      if (response.summaries && response.summaries.length > 0) {
        setSelectedSummary(response.summaries[0])
      }
    } catch (error) {
      console.error('Failed to fetch summaries:', error)
      toast.error('Failed to load work summaries')
    } finally {
      setLoading(false)
    }
  }

  const generateDailySummary = async () => {
    try {
      setGenerating(true)
      await apiClient.post('/api/work-summaries/generate-daily')
      toast.success('Daily summary generated successfully!')
      fetchSummaries()
    } catch (error) {
      console.error('Failed to generate daily summary:', error)
      toast.error('Failed to generate daily summary')
    } finally {
      setGenerating(false)
    }
  }

  const generateWeeklySummary = async () => {
    try {
      setGenerating(true)
      await apiClient.post('/api/work-summaries/generate-weekly')
      toast.success('Weekly summary generated successfully!')
      fetchSummaries()
    } catch (error) {
      console.error('Failed to generate weekly summary:', error)
      toast.error('Failed to generate weekly summary')
    } finally {
      setGenerating(false)
    }
  }

  const downloadSummary = (summary: WorkSummaryType) => {
    const content = `
Work Summary - ${summary.summaryType}
Date: ${new Date(summary.summaryDate).toLocaleDateString()}
User: ${user?.firstName} ${user?.lastName}

GitHub Activity:
${summary.githubActivity}

Meeting Participation:
${summary.meetingParticipation}

Documentation Work:
${summary.documentationWork}

Customer Support:
${summary.customerSupport}

AI Generated Summary:
${summary.aiGeneratedSummary}

Productivity Score: ${summary.productivityScore}%
Collaboration Score: ${summary.collaborationScore}%
    `.trim()

    const blob = new Blob([content], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `work-summary-${summary.summaryType.toLowerCase()}-${summary.summaryDate}.txt`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Work Summary</h1>
          <p className="text-gray-600">View and generate your work summaries</p>
        </div>
        <div className="flex space-x-3">
          <Select value={selectedTimeFilter} onValueChange={setSelectedTimeFilter}>
            <SelectTrigger className="w-48">
              <SelectValue placeholder="Select time period" />
            </SelectTrigger>
            <SelectContent>
              {timeFilters.map((filter) => (
                <SelectItem key={filter.value} value={filter.value}>
                  {filter.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button onClick={generateDailySummary} disabled={generating}>
            {generating ? <RefreshCw className="mr-2 h-4 w-4 animate-spin" /> : <RefreshCw className="mr-2 h-4 w-4" />}
            Generate Daily
          </Button>
          <Button onClick={generateWeeklySummary} disabled={generating} variant="outline">
            {generating ? <RefreshCw className="mr-2 h-4 w-4 animate-spin" /> : <RefreshCw className="mr-2 h-4 w-4" />}
            Generate Weekly
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle>Summary History</CardTitle>
            <CardDescription>
              Your recent work summaries
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {summaries.map((summary) => (
                <div
                  key={summary.id}
                  className={`p-3 border rounded-lg cursor-pointer transition-colors ${
                    selectedSummary?.id === summary.id ? 'bg-blue-50 border-blue-200' : 'hover:bg-gray-50'
                  }`}
                  onClick={() => setSelectedSummary(summary)}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="font-medium">{summary.summaryType} Summary</p>
                      <p className="text-sm text-gray-600">
                        {new Date(summary.summaryDate).toLocaleDateString()}
                      </p>
                    </div>
                    <Badge variant={summary.summaryType === 'DAILY' ? 'default' : 'secondary'}>
                      {summary.summaryType}
                    </Badge>
                  </div>
                  <div className="flex space-x-2 mt-2">
                    <Badge variant="outline" className="text-xs">
                      P: {summary.productivityScore}%
                    </Badge>
                    <Badge variant="outline" className="text-xs">
                      C: {summary.collaborationScore}%
                    </Badge>
                  </div>
                </div>
              ))}
              {summaries.length === 0 && (
                <p className="text-gray-500 text-center py-4">
                  No summaries found for this period
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <div className="flex justify-between items-center">
              <div>
                <CardTitle>
                  {selectedSummary ? `${selectedSummary.summaryType} Summary` : 'Select a Summary'}
                </CardTitle>
                <CardDescription>
                  {selectedSummary && new Date(selectedSummary.summaryDate).toLocaleDateString()}
                </CardDescription>
              </div>
              {selectedSummary && (
                <Button onClick={() => downloadSummary(selectedSummary)} variant="outline" size="sm">
                  <Download className="mr-2 h-4 w-4" />
                  Download
                </Button>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {selectedSummary ? (
              <Tabs defaultValue="overview" className="space-y-4">
                <TabsList>
                  <TabsTrigger value="overview">Overview</TabsTrigger>
                  <TabsTrigger value="details">Details</TabsTrigger>
                  <TabsTrigger value="metrics">Metrics</TabsTrigger>
                </TabsList>

                <TabsContent value="overview" className="space-y-4">
                  <Card>
                    <CardHeader>
                      <CardTitle className="flex items-center">
                        <TrendingUp className="mr-2 h-5 w-5" />
                        AI Generated Summary
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <Textarea
                        value={selectedSummary.aiGeneratedSummary}
                        readOnly
                        className="min-h-32 resize-none"
                      />
                    </CardContent>
                  </Card>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-lg">Productivity Score</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="text-3xl font-bold mb-2">{selectedSummary.productivityScore}%</div>
                        <Progress value={selectedSummary.productivityScore} className="mb-2" />
                        <p className="text-sm text-gray-600">Based on code commits, PRs, and task completion</p>
                      </CardContent>
                    </Card>

                    <Card>
                      <CardHeader>
                        <CardTitle className="text-lg">Collaboration Score</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="text-3xl font-bold mb-2">{selectedSummary.collaborationScore}%</div>
                        <Progress value={selectedSummary.collaborationScore} className="mb-2" />
                        <p className="text-sm text-gray-600">Based on meetings, reviews, and team interactions</p>
                      </CardContent>
                    </Card>
                  </div>
                </TabsContent>

                <TabsContent value="details" className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <Card>
                      <CardHeader>
                        <CardTitle className="flex items-center">
                          <GitBranch className="mr-2 h-5 w-5" />
                          GitHub Activity
                        </CardTitle>
                      </CardHeader>
                      <CardContent>
                        <Textarea
                          value={selectedSummary.githubActivity}
                          readOnly
                          className="min-h-24 resize-none"
                        />
                      </CardContent>
                    </Card>

                    <Card>
                      <CardHeader>
                        <CardTitle className="flex items-center">
                          <Calendar className="mr-2 h-5 w-5" />
                          Meeting Participation
                        </CardTitle>
                      </CardHeader>
                      <CardContent>
                        <Textarea
                          value={selectedSummary.meetingParticipation}
                          readOnly
                          className="min-h-24 resize-none"
                        />
                      </CardContent>
                    </Card>

                    <Card>
                      <CardHeader>
                        <CardTitle className="flex items-center">
                          <FileText className="mr-2 h-5 w-5" />
                          Documentation Work
                        </CardTitle>
                      </CardHeader>
                      <CardContent>
                        <Textarea
                          value={selectedSummary.documentationWork}
                          readOnly
                          className="min-h-24 resize-none"
                        />
                      </CardContent>
                    </Card>

                    <Card>
                      <CardHeader>
                        <CardTitle className="flex items-center">
                          <MessageSquare className="mr-2 h-5 w-5" />
                          Customer Support
                        </CardTitle>
                      </CardHeader>
                      <CardContent>
                        <Textarea
                          value={selectedSummary.customerSupport}
                          readOnly
                          className="min-h-24 resize-none"
                        />
                      </CardContent>
                    </Card>
                  </div>
                </TabsContent>

                <TabsContent value="metrics" className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">Summary Type</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <Badge variant="outline" className="text-lg">
                          {selectedSummary.summaryType}
                        </Badge>
                      </CardContent>
                    </Card>

                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">Created</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <p className="text-lg font-medium">
                          {new Date(selectedSummary.createdAt).toLocaleDateString()}
                        </p>
                        <p className="text-sm text-gray-600">
                          {new Date(selectedSummary.createdAt).toLocaleTimeString()}
                        </p>
                      </CardContent>
                    </Card>

                    <Card>
                      <CardHeader>
                        <CardTitle className="text-sm">Last Updated</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <p className="text-lg font-medium">
                          {new Date(selectedSummary.updatedAt).toLocaleDateString()}
                        </p>
                        <p className="text-sm text-gray-600">
                          {new Date(selectedSummary.updatedAt).toLocaleTimeString()}
                        </p>
                      </CardContent>
                    </Card>
                  </div>
                </TabsContent>
              </Tabs>
            ) : (
              <div className="text-center py-12">
                <Clock className="mx-auto h-12 w-12 text-gray-400" />
                <h3 className="mt-2 text-sm font-medium text-gray-900">No summary selected</h3>
                <p className="mt-1 text-sm text-gray-500">
                  Choose a summary from the list to view details
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default WorkSummary
