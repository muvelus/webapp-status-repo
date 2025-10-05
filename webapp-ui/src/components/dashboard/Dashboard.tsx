import React, { useState, useEffect } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card'
import { Button } from '../ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select'
import { Badge } from '../ui/badge'
import { Progress } from '../ui/progress'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { 
  Activity, 
  GitBranch, 
  MessageSquare, 
  FileText, 
  Users, 
  Calendar,
  TrendingUp,
  Clock
} from 'lucide-react'
import { WorkSummary, MeetingMinutes, TimeFilter } from '../../types'
import { apiClient } from '../../utils/api'
import { toast } from 'sonner'

const Dashboard: React.FC = () => {
  const { user } = useAuth()
  const [workSummaries, setWorkSummaries] = useState<WorkSummary[]>([])
  const [recentMeetings, setRecentMeetings] = useState<MeetingMinutes[]>([])
  const [loading, setLoading] = useState(true)
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
    }
  ]

  useEffect(() => {
    fetchDashboardData()
  }, [selectedTimeFilter])

  const fetchDashboardData = async () => {
    try {
      setLoading(true)
      const filter = timeFilters.find(f => f.value === selectedTimeFilter)
      if (!filter) return

      const [summariesResponse, meetingsResponse] = await Promise.all([
        apiClient.get<{ summaries: WorkSummary[] }>(`/api/work-summaries/my-summaries?startDate=${filter.startDate.toISOString()}&endDate=${filter.endDate.toISOString()}`),
        apiClient.get<{ meetings: MeetingMinutes[] }>(`/api/meetings/my-meetings?startDate=${filter.startDate.toISOString().split('T')[0]}&endDate=${filter.endDate.toISOString().split('T')[0]}`)
      ])

      setWorkSummaries(summariesResponse.summaries || [])
      setRecentMeetings(meetingsResponse.meetings || [])
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error)
      toast.error('Failed to load dashboard data')
    } finally {
      setLoading(false)
    }
  }

  const getProductivityData = () => {
    return workSummaries.map((summary, index) => ({
      name: `Day ${index + 1}`,
      productivity: summary.productivityScore,
      collaboration: summary.collaborationScore
    }))
  }

  const getActivityStats = () => {
    const totalSummaries = workSummaries.length
    const avgProductivity = workSummaries.reduce((acc, s) => acc + s.productivityScore, 0) / totalSummaries || 0
    const avgCollaboration = workSummaries.reduce((acc, s) => acc + s.collaborationScore, 0) / totalSummaries || 0
    const totalMeetings = recentMeetings.length

    return {
      totalSummaries,
      avgProductivity: Math.round(avgProductivity),
      avgCollaboration: Math.round(avgCollaboration),
      totalMeetings
    }
  }

  const stats = getActivityStats()

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
          <h1 className="text-3xl font-bold text-gray-900">
            Welcome back, {user?.firstName}!
          </h1>
          <p className="text-gray-600">Here's your work summary and recent activity</p>
        </div>
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
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Work Summaries</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.totalSummaries}</div>
            <p className="text-xs text-muted-foreground">
              Generated this period
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Avg Productivity</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.avgProductivity}%</div>
            <Progress value={stats.avgProductivity} className="mt-2" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Collaboration Score</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.avgCollaboration}%</div>
            <Progress value={stats.avgCollaboration} className="mt-2" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Meetings</CardTitle>
            <Calendar className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.totalMeetings}</div>
            <p className="text-xs text-muted-foreground">
              Attended this period
            </p>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="overview" className="space-y-4">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="activity">Activity</TabsTrigger>
          <TabsTrigger value="meetings">Recent Meetings</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Productivity Trends</CardTitle>
              <CardDescription>
                Your productivity and collaboration scores over time
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={getProductivityData()}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Line type="monotone" dataKey="productivity" stroke="#8884d8" strokeWidth={2} />
                  <Line type="monotone" dataKey="collaboration" stroke="#82ca9d" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="activity" className="space-y-4">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Recent Work Summaries</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {workSummaries.slice(0, 5).map((summary) => (
                    <div key={summary.id} className="flex items-center justify-between p-3 border rounded-lg">
                      <div>
                        <p className="font-medium">
                          {summary.summaryType} Summary
                        </p>
                        <p className="text-sm text-gray-600">
                          {new Date(summary.summaryDate).toLocaleDateString()}
                        </p>
                      </div>
                      <div className="flex space-x-2">
                        <Badge variant="outline">
                          P: {summary.productivityScore}%
                        </Badge>
                        <Badge variant="outline">
                          C: {summary.collaborationScore}%
                        </Badge>
                      </div>
                    </div>
                  ))}
                  {workSummaries.length === 0 && (
                    <p className="text-gray-500 text-center py-4">
                      No work summaries found for this period
                    </p>
                  )}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Quick Actions</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <Button className="w-full justify-start" variant="outline">
                    <GitBranch className="mr-2 h-4 w-4" />
                    Generate Daily Summary
                  </Button>
                  <Button className="w-full justify-start" variant="outline">
                    <FileText className="mr-2 h-4 w-4" />
                    View Work Summary
                  </Button>
                  <Button className="w-full justify-start" variant="outline">
                    <Calendar className="mr-2 h-4 w-4" />
                    Check Meeting Minutes
                  </Button>
                  <Button className="w-full justify-start" variant="outline">
                    <MessageSquare className="mr-2 h-4 w-4" />
                    Team Collaboration
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="meetings" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Recent Meetings</CardTitle>
              <CardDescription>
                Your recent meeting participation and minutes
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {recentMeetings.slice(0, 10).map((meeting) => (
                  <div key={meeting.id} className="flex items-center justify-between p-4 border rounded-lg">
                    <div className="flex-1">
                      <h4 className="font-medium">{meeting.meetingTitle}</h4>
                      <p className="text-sm text-gray-600">
                        {new Date(meeting.meetingDate).toLocaleDateString()} • {meeting.durationMinutes} min
                      </p>
                      <div className="flex items-center mt-2 space-x-2">
                        <Badge variant={meeting.processed ? 'default' : 'secondary'}>
                          {meeting.processed ? 'Processed' : 'Pending'}
                        </Badge>
                        <Badge variant="outline">
                          {meeting.meetingPlatform}
                        </Badge>
                      </div>
                    </div>
                    <Button variant="ghost" size="sm">
                      <Clock className="h-4 w-4" />
                    </Button>
                  </div>
                ))}
                {recentMeetings.length === 0 && (
                  <p className="text-gray-500 text-center py-4">
                    No meetings found for this period
                  </p>
                )}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}

export default Dashboard
